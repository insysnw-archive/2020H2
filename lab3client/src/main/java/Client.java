import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.Socket;



public class Client {

    JSONObject packets;
    private String username;
    private String password;
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

    private void signin(String username, String password) {
        JSONObject signupPacket = packets.getJSONObject("signinRequest");
        this.username = username;
        this.password = password;
        signupPacket.put("username", username);
        signupPacket.put("password", password);
        sendPacket(signupPacket.toString());
    }

    public void signup(String username, String password) {
        JSONObject signinPacket = packets.getJSONObject("signupRequest");
        this.username = username;
        this.password = password;
        signinPacket.put("username", username);
        signinPacket.put("password", password);
        sendPacket(signinPacket.toString());
    }

    public void wallets() {
        JSONObject clientMessage = packets.getJSONObject("walletListRequest");
        sendPacket(clientMessage.toString());
    }

    public void send(int walletId, int sum) {
        JSONObject clientMessage = packets.getJSONObject("send");
        clientMessage.put("walletId", walletId);
        clientMessage.put("sum", sum);
        sendPacket(clientMessage.toString());
    }

    private void checkWallet() {
        JSONObject clientMessage = packets.getJSONObject("checkWallet");
        sendPacket(clientMessage.toString());
    }

    public void quit() {
        JSONObject clientMessage = packets.getJSONObject("disconnectClient");
        sendPacket(clientMessage.toString());
    }

    private void sendRequest(String recipientUsername, int sum, int yourWalletId) {
        JSONObject clientMessage = packets.getJSONObject("sendRequest");
        clientMessage.put("sum", sum);
        clientMessage.put("sender", username);
        clientMessage.put("recipient", recipientUsername);
        clientMessage.put("walletId", yourWalletId);
        sendPacket(clientMessage.toString());
    }

    private void sendRequestResponse(int requestId, String answer) {
        JSONObject requestResponse = packets.getJSONObject("requestResponse");
        requestResponse.put("requestId", requestId);
        if(answer.equals("accept")) requestResponse.put("answer", true);
        else if (answer.equals("dent")) requestResponse.put("answer", false);
        else {
            System.out.println("You should enter either accept or deny");
            return;
        }
        sendPacket(requestResponse.toString());
    }

    public void startSendingThread() {
        Thread sending = new Thread(() -> {
            String clientString;
            while (connected) {
                try {
                    System.out.print("> ");
                    clientString = br.readLine();
                    switch (clientString) {
                        case "signup": {
                            System.out.println("Enter your username: ");
                            String username = br.readLine();
                            System.out.println("Enter your password: ");
                            String password = br.readLine();
                            signup(username, password);
                            break;
                        }
                        case "signin": {
                            System.out.println("Enter your username: ");
                            String username = br.readLine();
                            System.out.println("Enter your password: ");
                            String password = br.readLine();
                            signin(username, password);
                            break;
                        }
                        case "wallets": {
                            if(loggedIn) wallets();
                            else System.out.println("You are not logged in");
                            break;
                        }
                        case "send": {
                            try {
                                if(loggedIn) {
                                    System.out.println("Enter recipient's wallet id: ");
                                    int walletId = Integer.parseInt(br.readLine());
                                    System.out.println("Enter how much you want to send: ");
                                    int sum = Integer.parseInt(br.readLine());
                                    send(walletId, sum);
                                } else System.out.println("You are not logged in");
                            } catch (NumberFormatException e) {
                                System.out.println("Type in integers");
                            }
                            break;
                        }
                        case "check": {
                            if (loggedIn) {
                                checkWallet();
                            }
                            break;
                        }
                        case "quit": {
                            quit();
                            connected = false;
                            synchronized (socket) {
                                socket.notify();
                            }
                            break;
                        }
                        case "request": {
                            try {
                                if(loggedIn) {
                                    System.out.println("Enter recipient's username: ");
                                    String recipientUsername = br.readLine();
                                    System.out.println("Enter how much you're requesting: ");
                                    int sum = Integer.parseInt(br.readLine());
                                    System.out.println("Enter your wallet's id: ");
                                    int yourWalletId = Integer.parseInt(br.readLine());
                                    sendRequest(recipientUsername, sum, yourWalletId);
                                } else System.out.println("You are not logged in");
                            } catch (NumberFormatException e) {
                                System.out.println("Type in integers");
                            }
                            break;
                        }
                        case "requests": {
                            if(loggedIn) {
                                sendPacket(packets.getJSONObject("checkRequests").toString());
                            }
                            break;
                        }
                        case "answer": {
                            try {
                                if(loggedIn) {
                                    System.out.println("Enter request's id: ");
                                    int requestId = Integer.parseInt(br.readLine());
                                    System.out.println("Enter your answer (accept/deny): ");
                                    String answer = br.readLine();
                                    sendRequestResponse(requestId, answer);
                                } else System.out.println("You are not logged in");
                            } catch (NumberFormatException e) {
                                System.out.println("Type in integers");
                            }
                            break;
                        }
                        default:
                            System.out.println("Use on of the following commands: \n" +
                                    "signup\n" +
                                    "signin\n" +
                                    "wallets - requests a list of wallets\n" +
                                    "send - to send money to someone\n" +
                                    "check - to check your balance\n" +
                                    "request - to request some amount from someone\n" +
                                    "requests - to get the list of requests from users\n" +
                                    "answer - to answer to request\n" +
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
                JSONObject serverMessage;
                try {
                    serverMessage = new JSONObject(new JSONTokener(din.readUTF()));
                    int opcode = serverMessage.getInt("opcode");
                    if(opcode == Opcode.DISCONNECT.getOpcode()) {
                        System.out.println(serverMessage.get("message"));
                        connected = false;
                        loggedIn = false;
                        synchronized (socket) {
                            socket.notify();
                        }
                    }
                    if(loggedIn) {
                        if (opcode == (Opcode.ERROR.getOpcode())) {
                            System.out.println(serverMessage.get("message"));
                        } else if (opcode == (Opcode.ANSWERWALLET.getOpcode())) {
                            System.out.println("Your balance: " + serverMessage.get("balance") + " on wallet: " + serverMessage.get("walletId"));
                        } else if (opcode == (Opcode.SENDSTATUS.getOpcode())) {
                            if (serverMessage.getBoolean("successful")) {
                                System.out.println(serverMessage.get("recipient") + " received " + serverMessage.get("sum") + " successfully");
                                System.out.println("You have " + serverMessage.get("balance") + " left");
                            } else {
                                System.out.println(serverMessage.get("recipient") + "didn't receive your money");
                            }
                        } else if (opcode == (Opcode.WALLETLIST.getOpcode())) {
                            JSONArray walletArray = serverMessage.getJSONArray("wallets");
                            JSONObject wallet;
                            for (int i = 0; i < walletArray.length(); i++) {
                                wallet = walletArray.getJSONObject(i);
                                System.out.println(wallet.get("owner") + "\t" + wallet.get("uid"));
                            }
                        } else if(opcode == Opcode.SINGUP.getOpcode() || opcode == Opcode.SIGNIN.getOpcode()) {
                            System.out.println("You are already logged in");
                        } else if(opcode == Opcode.RECEIVESTATUS.getOpcode()) {
                            System.out.println("You've received " + serverMessage.get("sum")
                                                + " from " + serverMessage.get("sender"));
                            System.out.println("Your current balance is " + serverMessage.get("balance"));
                        } else if(opcode == Opcode.SENDREQUEST.getOpcode()) {
                            System.out.println(serverMessage.get("sender") + " requests for " + serverMessage.get("sum"));
                            System.out.println("To send type in \"accept\" to deny - \"deny\"");
                        } else if(opcode == (Opcode.REQUESTLIST.getOpcode())) {
                            JSONArray requestArray = serverMessage.getJSONArray("requests");
                            JSONObject request;
                            for (int i = 0; i < requestArray.length(); i++) {
                                request = requestArray.getJSONObject(i);
                                System.out.println("request id: " + request.get("RequestId") + "\tsender: " + request.get("sender"));
                            }
                        }
                    } else {
                        if (opcode == (Opcode.RESPONSE.getOpcode())) {
                            if (serverMessage.getBoolean("accepted")) {
                                loggedIn = true;
                            } else {
                                System.out.println(serverMessage.get("message"));
                            }
                        } else {
                            System.out.println("You need to log in first (signup/signin");
                        }
                    }
                } catch (IOException e) {
                    if(!connected) {
                        System.out.println("Disconnected");
                    } else e.printStackTrace();
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
