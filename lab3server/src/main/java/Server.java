import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {
    private final DatagramSocket socket;
    private final Map<String, Client> clients = new HashMap<>();
    private final Map<String, Lot> lots = new HashMap<>();
    public static JSONObject packets;
    public static final int MSG_LIM = 512;
    public static final int PORT = 3333;
    private boolean working = true;

    public Server(int port) throws SocketException {
        packets = new JSONObject(new JSONTokener(Server.class.getResourceAsStream("/packets.json")));
        socket = new DatagramSocket(port);
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(PORT);
            server.service();
        } catch (SocketException ex) {
            System.out.println("Server closed");
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private void service() throws IOException {
        startCommandThread();
        while (working) {
            DatagramPacket request = new DatagramPacket(new byte[MSG_LIM], MSG_LIM);
            socket.receive(request);
            requestHandler(request);
        }
    }

    private void startCommandThread() {
        Thread commandThread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String command;
            while (working) {
                try {
                    command = br.readLine();
                    switch (command) {
                        case "clients": {
                            for(Client client: clients.values()) {
                                System.out.println(client.toString());
                            }
                            break;
                        }
                        case "disconnect": {
                            System.out.println("Enter client's username: ");
                            String name = br.readLine();
                            for(Client client: clients.values()) {
                                if(client.getUsername().equals(name)) {
                                    disconnectClient(client.getClientAddress(), client.getClientPort());
                                }
                            }
                            break;
                        }
                        case "quit": {
                            socket.close();
                            working = false;
                            break;
                        }
                        default: {
                            System.out.println("clients - client list of connected clients\n" +
                                    "disconnect - disconnect particular client\n" +
                                    "quit - close server");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        commandThread.start();
    }

    private void requestHandler(DatagramPacket request) {
        Thread handler = new Thread(() -> {
            int opcode;
            JSONObject requestPacket = new JSONObject(new String(request.getData()));
            opcode = requestPacket.getInt("opcode");
            try {
                if(opcode != 0 && !clients.containsKey(request.getAddress().toString() + request.getPort())) {
                    JSONObject jsonResponse = new JSONObject(packets.getJSONObject("error"));
                    jsonResponse.put("code", 403);
                    jsonResponse.put("message", "You must log in first");
                    sendResponse(jsonResponse, request.getAddress(), request.getPort());
                } else {
                    switch (opcode) {
                        case 0: {
                            connectClient(requestPacket, request.getAddress(), request.getPort());
                            break;
                        }
                        case 2: {
                            sendLotList(request.getAddress(), request.getPort());
                            break;
                        }
                        case 4: {
                            raiseBid(requestPacket, request.getAddress(), request.getPort());
                            break;
                        }
                        case 6: {
                            newLot(requestPacket, request.getAddress(), request.getPort());
                            break;
                        }
                        case 8: {
                            endTrade(requestPacket, request.getAddress(), request.getPort());
                            break;
                        }
                        case 10: {
                            disconnectClient(request.getAddress(), request.getPort());
                            break;
                        }
                        default:
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        handler.start();
    }

    private synchronized void disconnectClient(InetAddress address, int port) throws IOException {
        JSONObject jsonResponse;

        Client client = clients.get(address.toString() + port);
        if(client.getRole() == Role.SELLER) {
            for(Lot lot: lots.values()) {
                if(lot.getOwner().equals(client)) {
                    lots.remove(lot.getLotId());
                    for (Client connectedClient: clients.values()) {
                        if(connectedClient.getRole().equals(Role.BUYER)) {
                            connectedClient.getBidLots().remove(lot.getLotId());
                        }
                    }
                }
            }
        }
        clients.remove(address.toString() + port);
        jsonResponse = new JSONObject(packets.getJSONObject("serverReply"));
        sendResponse(jsonResponse, address, port);
    }

    private synchronized void endTrade(JSONObject requestPacket, InetAddress address, int port) throws IOException {
        JSONObject jsonResponse;
        String lotId = requestPacket.getString("lotId");
        Lot lot = new Lot(lots.get(lotId));
        if(lots.containsKey(lotId) && lot.getOwner().equals(clients.get(address.toString() + port))) {
            jsonResponse = new JSONObject(packets.getJSONObject("endTradeResults"));
            jsonResponse.put("lotId", lotId);
            jsonResponse.put("lotName", lot.getLotName());
            jsonResponse.put("lotOwner", lot.getOwner().getUsername());
            jsonResponse.put("highestBid", lot.getCurrentPrice());
            sendResponse(jsonResponse, address, port);
            for(Client client: clients.values()) {
                if(client.getBidLots().containsKey(lotId)) {
                    sendResponse(jsonResponse, client.getClientAddress(), client.getClientPort());
                    client.getBidLots().remove(lotId);
                }
            }
            lots.remove(lotId);
        } else {
            jsonResponse = new JSONObject(packets.getJSONObject("error"));
            jsonResponse.put("code", 404);
            jsonResponse.put("message", "Lot with that id doesn't exist");
        }
    }

    private void newLot(JSONObject requestPacket, InetAddress address, int port) throws IOException {
        JSONObject jsonResponse;
        Lot lot = new Lot(clients.get(address.toString() + port),
                requestPacket.getInt("lotInitialPrice"),
                requestPacket.getString("lotName"));
        if(clients.get(address.toString() + port).getRole() == Role.SELLER) {
            if (!lots.containsKey(lot.getLotId())) {
                lots.put(lot.getLotId(), lot);
                jsonResponse = new JSONObject(packets.getJSONObject("serverReply"));
            } else {
                jsonResponse = new JSONObject(packets.getJSONObject("error"));
                jsonResponse.put("code", 105);
                jsonResponse.put("message", "Such lot already exists");
            }
        } else {
            jsonResponse = new JSONObject(packets.getJSONObject("error"));
            jsonResponse.put("code", 106);
            jsonResponse.put("message", "You must be a seller to add new lots");
        }
        sendResponse(jsonResponse, address, port);
    }

    private synchronized void raiseBid(JSONObject requestPacket, InetAddress address, int port) throws IOException {
        String lotId = requestPacket.getString("lotId");
        JSONObject jsonResponse;
        JSONObject jsonResponseToParticipants;
        if(clients.get(address.toString() + port).getRole().equals(Role.BUYER)) {
            if (lots.containsKey(lotId)) {
                if (lots.get(lotId).setCurrentPrice(requestPacket.getInt("bid"))) {
                    clients.get(address.toString() + port).getBidLots().put(lotId, lots.get(lotId));
                    jsonResponse = new JSONObject(packets.getJSONObject("serverReply"));
                    jsonResponseToParticipants = new JSONObject(packets.getJSONObject("lotPriceUpdate"));
                    jsonResponseToParticipants.put("lotName", lots.get(lotId).getLotName());
                    jsonResponseToParticipants.put("lotId", lotId);
                    jsonResponseToParticipants.put("lotCurrentPrice", lots.get(lotId).getCurrentPrice());
                    lots.get(lotId).setHighestBidder(clients.get(address.toString() + port));
                    sendResponse(jsonResponseToParticipants, lots.get(lotId).getOwner().getClientAddress(), lots.get(lotId).getOwner().getClientPort());
                    for(Client client: clients.values()) {
                        if(client.getBidLots().containsKey(lotId)) {
                            sendResponse(jsonResponseToParticipants, client.getClientAddress(), client.getClientPort());
                        }
                    }
                } else {
                    jsonResponse = new JSONObject(packets.getJSONObject("error"));
                    jsonResponse.put("code", 102);
                    jsonResponse.put("message", "Your bid must be higher than current price");
                }
            } else {
                jsonResponse = new JSONObject(packets.getJSONObject("error"));
                jsonResponse.put("code", 404);
                jsonResponse.put("message", "Lot with that id doesn't exist");
            }
        } else {
            jsonResponse = new JSONObject(packets.getJSONObject("error"));
            jsonResponse.put("code", 103);
            jsonResponse.put("message", "You must be a buyer to place bids");
        }
        sendResponse(jsonResponse, address, port);
    }

    private void sendLotList(InetAddress address, int port) throws IOException {
        JSONObject jsonResponse = new JSONObject(packets.getJSONObject("lotListReply"));
        List<Lot> tempLots = new ArrayList<>(lots.values());
        for(int i = 0; i < lots.size(); i++) {
            jsonResponse.getJSONArray("lots").put(i, tempLots.get(i).toJsonObject());
        }
        sendResponse(jsonResponse, address, port);
    }

    private void connectClient(JSONObject connectRequest, InetAddress address, int port) throws IOException {
        Role role = connectRequest.getBoolean("buyer") ? Role.BUYER : Role.SELLER;
        Client client = new Client(connectRequest.getString("username"), role, address, port);
        JSONObject jsonResponse;
        if (clients.containsKey(address.toString() + port)) {
            jsonResponse = new JSONObject(packets.getJSONObject("error"));
            jsonResponse.put("code", 101);
            jsonResponse.put("message", "User already connected");
        } else {
            jsonResponse = new JSONObject(packets.getJSONObject("serverReply"));
            clients.put(address.toString() + port, client);
        }
        sendResponse(jsonResponse, client.getClientAddress(), client.getClientPort());
    }

    private void sendResponse(JSONObject jsonResponse, InetAddress clientAddress, int clientPort) throws IOException {
        String stringResponse = jsonResponse.toString();
        DatagramPacket response = new DatagramPacket(stringResponse.getBytes(StandardCharsets.UTF_8),
                stringResponse.getBytes(StandardCharsets.UTF_8).length,
                clientAddress,
                clientPort);
        socket.send(response);
    }
}