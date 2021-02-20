import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.Socket;

public class Client {
    private Role role;

    JSONObject packets;
    private String username;
    private Socket socket;
    private DataInputStream din;
    private DataOutputStream dout;
    private Boolean loggedIn = false;
    private final BufferedReader br;
    private boolean connected = false;

    public Client() {
        this.packets = new JSONObject(new JSONTokener(Client.class.getResourceAsStream("/packets.json")));
        this.br = new BufferedReader(new InputStreamReader(System.in));
    }

    public void connect() throws IOException {
        String ipAddress = null;
        String port = null;
        while (!connected) {
            System.out.println("Enter ip address");
            ipAddress = br.readLine();
            System.out.println("Enter port");
            port = br.readLine();
            try {
                socket = new Socket(ipAddress, Integer.parseInt(port));
                connected = true;
            } catch (IOException e) {
                System.out.println("Couldn't connect");
                connected = false;
            }
        }
        din = new DataInputStream(socket.getInputStream());
        dout = new DataOutputStream(socket.getOutputStream());
        System.out.println("Connected to " + ipAddress + ":" + port);
        startSendingThread();
        startReceivingThread();
        while (connected) {
            try {
                synchronized (socket) {
                    socket.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        dout.close();
        din.close();
        socket.close();
    }

    public void sendPacket(String packet) {
        try {
            dout.writeUTF(packet);
            dout.flush();
        } catch (IOException e) {
            e.printStackTrace();
            if(!socket.isConnected()) {
                socket.notify();
            }
        }
    }

    private void signin(String username, Role role) {
        JSONObject signinPacket = new JSONObject(packets.getJSONObject("signinRequest"));
        this.username = username;
        this.role = role;
        signinPacket.put("username", username);
        signinPacket.put("buyer", role.equals(Role.BUYER));
        signinPacket.put("opcode", 0);
        sendPacket(signinPacket.toString());
    }

    private void lotList() {
        JSONObject packet = new JSONObject(packets.getJSONObject("lotListRequest"));
        packet.put("opcode", 2);
        sendPacket(packet.toString());
    }

    private void raiseBid(String lotId, int bid){
        JSONObject packet = new JSONObject(packets.getJSONObject("raiseBidRequest"));
        packet.put("lotId", lotId);
        packet.put("bid", bid);
        packet.put("opcode", 4);
        sendPacket(packet.toString());
    }

    private void newLot(String name, int bid) {
        JSONObject packet = new JSONObject(packets.getJSONObject("newLotRequest"));
        packet.put("lotName", name);
        packet.put("lotInitialPrice", bid);
        packet.put("opcode", 6);
        sendPacket(packet.toString());
    }

    private void endTrade(String id) {
        JSONObject packet = new JSONObject(packets.getJSONObject("endTradeRequest"));
        packet.put("lotId", id);
        packet.put("opcode", 8);
        sendPacket(packet.toString());
    }

    public void quit() {
        JSONObject packet = new JSONObject(packets.getJSONObject("disconnectRequest"));
        packet.put("opcode", 10);
        sendPacket(packet.toString());
    }

    public void startSendingThread() {
        Thread sending = new Thread(() -> {
            String clientString;
            while (connected) {
                try {
                    System.out.print("> ");
                    clientString = br.readLine();
                    switch (clientString) {
                        case "signin": {
                            System.out.println("Enter your username: ");
                            while (true) {
                                String username = br.readLine();
                                System.out.println("Are you buyer? (yes/no) \n If not, you will be signed as seller: ");
                                if (br.readLine().equals("yes")){
                                    role = Role.BUYER;
                                    break;
                                }
                                else if (br.readLine().equals("no")){
                                    role = Role.SELLER;
                                    break;
                                }
                                else {
                                    System.out.println("Please, answer yes/no");
                                }
                            }

                            signin(username, role);
                            break;
                        }
                        case "list": {
                            if(loggedIn) lotList();
                                else System.out.println("You are not logged in");
                            break;
                        }
                        case "raise": {
                            try {
                                if(loggedIn && role.equals(Role.BUYER)) {
                                    System.out.println("Enter lot ID: ");
                                    String lotId = br.readLine();
                                    System.out.println("Enter how much you want to bid: ");
                                    int bid = Integer.parseInt(br.readLine());
                                    raiseBid(lotId, bid);
                                } else System.out.println("You are not logged in or not presented as buyer");
                            } catch (NumberFormatException e) {
                                System.out.println("Please, type only non-float numbers for bid");
                            }
                            break;
                        }
                        case "newlot": {
                            try {
                                if (loggedIn && role.equals(Role.SELLER)) {
                                    System.out.println("Enter lot name: ");
                                    String name = br.readLine();
                                    System.out.println("Enter how much you want to bid: ");
                                    int bid = Integer.parseInt(br.readLine());
                                    newLot(name, bid);
                                } else System.out.println("You are not logged in as Seller");
                                break;
                            }catch (NumberFormatException e){
                                System.out.println("Price must be given with non-float number");
                            }

                        }
                        case "quit": {
                            quit();
                            connected = false;
                            synchronized (socket) {
                                socket.notify();
                            }
                            break;
                        }
                        /*
                        case "update": {
                            try {
                                if (loggedIn && role.equals(Role.SELLER)) {
                                    System.out.println("Enter lot name: ");
                                    String name = br.readLine();
                                    System.out.println("Enter new price: ");
                                    int newPrice = Integer.parseInt(br.readLine());
                                    updatePrice(name, newPrice);
                                } else System.out.println("You are not logged in as Seller");
                            } catch (NumberFormatException e) {
                                System.out.println("Type in integers");
                            }
                            break;
                        }*/
                        case "endtrade": {
                            if(loggedIn) {
                                System.out.println("Enter lot ID of which you want to decline: ");
                                String id = br.readLine();
                                endTrade(id);
                            }
                            break;
                        }
                        default:
                            System.out.println("Use on of the following commands: \n" +
                                    "signin\n" +
                                    "list - to get the list of lots\n" +
                                    "raise - to raise a bid as buyer\n" +
                                    "newlot - to form a new lot as seller\n" +
                                    "quit - to disconnect\n" +
                                    "update - to update lot as seller\n" +
                                    "endtrade - to end trade as seller\n" +
                                    "quit");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        sending.start();
    }

    public void startReceivingThread() {
        Thread receiving = new Thread(() -> {
            while (connected) {
                JSONObject serverReply;
                try {
                    serverReply = new JSONObject(new JSONTokener(din.readUTF()));
                    int opcode = serverReply.getInt("opcode");
                    if(opcode == 10) { //disconnect
                        System.out.println("Server disconnected you from session");
                        connected = false;
                        loggedIn = false;
                        synchronized (socket) {
                            socket.notify();
                        }
                    }
                    if(loggedIn) {
                        switch (opcode) {
                            case 5: { //error
                                System.out.println(serverReply.get("message"));
                                break;
                            }
                            case 3: { //lotListReply
                                JSONArray lotsArray = serverReply.getJSONArray("lots");
                                JSONObject lot;
                                for (int i = 0; i < lotsArray.length(); i++) {
                                    lot = lotsArray.getJSONObject(i);
                                    System.out.println("lot id: " + lot.get("lotId") + "\tname: " + lot.get("lotName")
                                            + "\tseller: " + lot.get("seller") + "\tinitial price: "
                                            + lot.get("lotInitialPrice") + "\tcurrent: " + lot.get("lotCurrentPrice"));
                                }
                                break;
                            }
                            case 1: { //server reply
                                if (serverReply.get("accepted").equals(true))
                                    System.out.println("\t Operation Successful");
                                else
                                    System.out.println("\t Not accepted by server");
                                break;
                            }
                            case 9: { //end trade results
                                System.out.println(serverReply.get("Trade has ended. Result:"));
                                System.out.println("\tLot ID: " + serverReply.get("lotId"));
                                System.out.println("\tLot name: " + serverReply.get("lotName"));
                                System.out.println("\tOwner: " + serverReply.get("lotOwner"));
                                System.out.println("\tWon with the highest bid: " + serverReply.get("highestBid"));
                                break;
                            }
                        }
                    }
                    else {
                        if (opcode == 1) {//server reply
                            if (serverReply.get("accepted").equals(true)) {
                                loggedIn = true;
                                System.out.println("\t Successful login");
                            } else
                                System.out.println("\t Could not login to server, this name is reserved");
                        }
                    }
                } catch (IOException e) {
                    if(!connected) {
                        System.out.println("Disconnected");
                    } else {
                        System.out.println("Something's wrong with received message");
                        e.printStackTrace();
                    }
                }
            }
        });
        receiving.start();
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.connect();
    }
}