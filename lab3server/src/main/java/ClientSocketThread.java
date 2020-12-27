import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClientSocketThread extends Thread{

    private final BufferedReader din;
    private final BufferedWriter dout;
    private Client client;
    private final InetAddress address;
    private final int port;
    private final Socket socket;
    private boolean connected = true;

    public BufferedWriter getDout() {
        return dout;
    }

    public Socket getSocket() {
        return socket;
    }

    public Client getClient() {
        return client;
    }


    public ClientSocketThread(Socket socket) throws IOException {
        this.socket = socket;
        this.din = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.dout = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.address = socket.getInetAddress();
        this.port = socket.getPort();
    }

    @Override
    public void run() {
        super.run();
        int opcode;
        while (connected) {
            try {
                JSONObject requestPacket = new JSONObject(din.read());
                opcode = requestPacket.getInt("opcode");
                if (opcode != 0 && Server.clients.containsKey(address.toString() + port)) {
                    JSONObject jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
                    jsonResponse.put("code", 403);
                    jsonResponse.put("message", "You must log in first");
                    sendPacket(jsonResponse.toString());
                } else {
                    switch (opcode) {
                        case 0: {
                            connectClient(requestPacket);
                            break;
                        }
                        case 2: {
                            sendLotList();
                            break;
                        }
                        case 4: {
                            raiseBid(requestPacket);
                            break;
                        }
                        case 6: {
                            newLot(requestPacket);
                            break;
                        }
                        case 8: {
                            endTrade(requestPacket);
                            break;
                        }
                        case 10: {
                            disconnectClient();
                            break;
                        }
                        default:
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendPacket(String packet) {
        try {
            dout.write(packet);
            dout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void disconnectClient() throws IOException {
        JSONObject jsonResponse;

        if(client.getRole() == Role.SELLER) {
            for(Lot lot: client.getBidLots().values()) {
                Server.lots.remove(lot.getLotId());
                for (ClientSocketThread connectedClient: Server.clients.values()) {
                    if(connectedClient.getClient().getRole().equals(Role.BUYER)) {
                        connectedClient.getClient().getBidLots().remove(lot.getLotId());
                    }
                }
            }
        }
        Server.clients.remove(address.toString() + port);
        jsonResponse = new JSONObject(Server.packets.getJSONObject("serverReply"));
        sendPacket(jsonResponse.toString());
        din.close();
        dout.close();
        socket.close();
        connected = false;
    }

    private synchronized void endTrade(JSONObject requestPacket) throws IOException {
        JSONObject jsonResponse;
        String lotId = requestPacket.getString("lotId");
        if(client.getRole().equals(Role.SELLER) && client.getBidLots().containsKey(lotId)) {
            Lot lot = new Lot(Server.lots.get(lotId));
            jsonResponse = new JSONObject(Server.packets.getJSONObject("endTradeResults"));
            jsonResponse.put("lotId", lotId);
            jsonResponse.put("lotName", lot.getLotName());
            jsonResponse.put("lotOwner", lot.getOwner().getUsername());
            jsonResponse.put("highestBid", lot.getCurrentPrice());
            sendPacket(jsonResponse.toString());
            for(ClientSocketThread serverClient: Server.clients.values()) {
                if(serverClient.getClient().getBidLots().containsKey(lotId)) {
                    serverClient.getDout().write(jsonResponse.toString());
                    serverClient.getClient().getBidLots().remove(lotId);
                }
            }
            Server.lots.remove(lotId);
        } else {
            jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
            jsonResponse.put("code", 404);
            jsonResponse.put("message", "Lot with that id doesn't exist");
            sendPacket(jsonResponse.toString());
        }
    }

    private void newLot(JSONObject requestPacket) throws IOException {
        JSONObject jsonResponse;
        Lot lot = new Lot(client,
                requestPacket.getInt("lotInitialPrice"),
                requestPacket.getString("lotName"));
        if(client.getRole().equals(Role.SELLER)) {
            if (!Server.lots.containsKey(lot.getLotId())) {
                Server.lots.put(lot.getLotId(), lot);
                jsonResponse = new JSONObject(Server.packets.getJSONObject("serverReply"));
            } else {
                jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
                jsonResponse.put("code", 105);
                jsonResponse.put("message", "Such lot already exists");
            }
        } else {
            jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
            jsonResponse.put("code", 106);
            jsonResponse.put("message", "You must be a seller to add new lots");
        }
        sendPacket(jsonResponse.toString());
    }

    private synchronized void raiseBid(JSONObject requestPacket) throws IOException {
        String lotId = requestPacket.getString("lotId");
        JSONObject jsonResponse;
        JSONObject jsonResponseToParticipants;
        if(client.getRole().equals(Role.BUYER)) {
            if (Server.lots.containsKey(lotId)) {
                if (Server.lots.get(lotId).setCurrentPrice(requestPacket.getInt("bid"))) {
                    client.getBidLots().put(lotId, Server.lots.get(lotId));
                    jsonResponse = new JSONObject(Server.packets.getJSONObject("serverReply"));
                    jsonResponseToParticipants = new JSONObject(Server.packets.getJSONObject("lotPriceUpdate"));
                    jsonResponseToParticipants.put("lotName", Server.lots.get(lotId).getLotName());
                    jsonResponseToParticipants.put("lotId", lotId);
                    jsonResponseToParticipants.put("lotCurrentPrice", Server.lots.get(lotId).getCurrentPrice());
                    Server.lots.get(lotId).setHighestBidder(client);
                    sendPacket(jsonResponse.toString());
                    for(ClientSocketThread client: Server.clients.values()) {
                        if(client.getClient().getBidLots().containsKey(lotId)) {
                            client.getDout().write(jsonResponseToParticipants.toString());
                        }
                    }
                } else {
                    jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
                    jsonResponse.put("code", 102);
                    jsonResponse.put("message", "Your bid must be higher than current price");
                }
            } else {
                jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
                jsonResponse.put("code", 404);
                jsonResponse.put("message", "Lot with that id doesn't exist");
            }
        } else {
            jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
            jsonResponse.put("code", 103);
            jsonResponse.put("message", "You must be a buyer to place bids");
        }
        sendPacket(jsonResponse.toString());
    }

    private void sendLotList() throws IOException {
        JSONObject jsonResponse = new JSONObject(Server.packets.getJSONObject("lotListReply"));
        List<Lot> tempLots = new ArrayList<>(client.getBidLots().values());
        for(int i = 0; i < tempLots.size(); i++) {
            jsonResponse.getJSONArray("lots").put(i, tempLots.get(i).toJsonObject());
        }
        sendPacket(jsonResponse.toString());
    }

    private void connectClient(JSONObject connectRequest) throws IOException {
        Role role = connectRequest.getBoolean("buyer") ? Role.BUYER : Role.SELLER;
        client = new Client(connectRequest.getString("username"), role, address, port);
        JSONObject jsonResponse;
        if (Server.clients.containsKey(address.toString() + port)) {
            jsonResponse = new JSONObject(Server.packets.getJSONObject("error"));
            jsonResponse.put("code", 101);
            jsonResponse.put("message", "User already connected");
        } else {
            jsonResponse = new JSONObject(Server.packets.getJSONObject("serverReply"));
            Server.clients.put(address.toString() + port, this);
        }
        sendPacket(jsonResponse.toString());
    }
}
