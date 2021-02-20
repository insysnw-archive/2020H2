package server;

import java.io.*;
import java.net.Socket;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class ServerSession {
    JSONObject packets;
    private static final String PATH_TO_CLIENTS_INFO = "src/main/resources/ClientsInfo";
    private static final String ERR_SIGN = "Wrong login";
    private static final String WRONG_FORMAT = "Wrong format";
    private Socket socket;
    private Server server;
    private BufferedReader in;
    private BufferedWriter out;


    private DataInputStream din;
    private DataOutputStream dout;

    private User user;
    private boolean connected;
    private Condition condition;
    private ServerSession from;
    private int amount;

    public ServerSession(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        connected = true;
        din = new DataInputStream(socket.getInputStream());
        dout = new DataOutputStream(socket.getOutputStream());
        condition = Condition.NORMAL;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.packets = new JSONObject(new JSONTokener(ServerSession.class.getResourceAsStream("/packets.json")));
        System.out.println("Started server " + "127.0.0.1" + ":" + Server.PORT);
        run();
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

    public void run() {
        new Thread(() -> {
            while (connected) {
                JSONObject clientMessage;
                try {
                    try {
                        clientMessage = new JSONObject(new JSONTokener(din.readUTF()));
                        System.out.println(clientMessage.toString());
                        int opcode = clientMessage.getInt("opcode");
                        if(opcode == Opcode.DISCONNECT.getOpcode()) {
                            System.out.println(clientMessage.get("message"));
                            connected = false;
                            server.removeClient(this);
                            synchronized (socket) {
                                socket.notify();
                            }

                        }


                    String msg = in.readLine();
                    if (msg == null) {
                        disconnect();
                        return;
                    }
                    if (user != null) System.out.println("from " + user.getLogin() + ": " + msg);
                    switch (condition) {
                        case NORMAL:
                            try {
                                normalHandler(clientMessage);
                            } catch (NumberFormatException e) {
                                JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
                                errorPacket.put("message", WRONG_FORMAT);
                                from.sendPacket(errorPacket.toString());
                            }
                            break;
                        case ACCEPTING:
                            acceptingHandler(msg);
                            break;

                        case WAITING_FOR_ACCEPT:
                            JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
                            errorPacket.put("message", "Can't do this operation\nWaiting for accept...");
                            from.sendPacket(errorPacket.toString());
                    }
                } catch (IOException e) {
                    this.downService();
                }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    private void normalHandler(JSONObject msg) throws IOException, NumberFormatException {
        JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
        errorPacket.put("message", WRONG_FORMAT);
        Opcode opcode = Opcode.valueOf(msg.getString("opcode"));
        switch (opcode) {
            case SIGNIN:
                signin(msg.getString("username"), msg.getString("password"));
                break;
            case SIGNUP:
                signup(msg.getString("username"), msg.getString("password"));
                break;
            case CHECKWALLET:
                getInfo();
                break;
            case WALLETLIST:
                getList();
                break;
            case SEND:
                transaction(msg.getInt("id"), msg.getInt("amount"));
                break;
            /*case server.Opcode.SEND:
                if (strings.length != 2) {
                    from.sendPacket(errorPacket.toString());
                } else {
                    user.setAmount(user.getAmount() + Integer.parseInt(strings[1]));
                    Utils.updateUserSizeInfo(user, PATH_TO_CLIENTS_INFO);
                    JSONObject responsePacket = new JSONObject(packets.getJSONObject("response"));
                    responsePacket.put("accepted", true);
                    responsePacket.put("message", "Complete");
                }
                break;*/
            case SENDREQUEST:
                int askedAmount = msg.getInt("amount");
                if (user.getAmount() < askedAmount) {
                    JSONObject packet = new JSONObject(packets.getJSONObject("error"));
                    packet.put("message", "Not enough money");
                    from.sendPacket(packet.toString());
                } else {
                    user.setAmount(user.getAmount() - askedAmount);
                    Utils.updateUserSizeInfo(user, PATH_TO_CLIENTS_INFO);
                    JSONObject responsePacket = new JSONObject(packets.getJSONObject("response"));
                    responsePacket.put("accepted", true);
                    responsePacket.put("message", "Complete");
                }
                break;
            case DISCONNECT:
                JSONObject packet = new JSONObject(packets.getJSONObject("disconnectClient"));
                packet.put("message", "Client logged out");
                from.sendPacket(packet.toString());
                disconnect();
                break;
            default:
                JSONObject error = new JSONObject(packets.getJSONObject("error"));
                error.put("message", "Unknown command");
                from.sendPacket(error.toString());
        }
    }

    private void acceptingHandler(String msg) throws IOException {
        switch (msg) {
            case "yes":
                user.setAmount(user.getAmount() - amount);
                Utils.updateUserSizeInfo(user, PATH_TO_CLIENTS_INFO);
                from.getUser().setAmount(from.getUser().getAmount() + amount);
                Utils.updateUserSizeInfo(from.getUser(), PATH_TO_CLIENTS_INFO);
                JSONObject accepted = new JSONObject(packets.getJSONObject("response"));
                accepted.put("accepted", true);
                accepted.put("message", "Transaction completed");
                from.sendPacket(accepted.toString());
                sendPacket(accepted.toString());
                from.setCondition(Condition.NORMAL);
                condition = Condition.NORMAL;
                break;
            case "no":
                JSONObject denied = new JSONObject(packets.getJSONObject("response"));
                denied.put("accepted", false);
                denied.put("message", "Transaction denied");
                from.sendPacket(denied.toString());
                from.setCondition(Condition.NORMAL);
                condition = Condition.NORMAL;
                break;
            default:
                JSONObject error = new JSONObject(packets.getJSONObject("error"));
                error.put("message", "Unknown command\n Accept (yes/no)");
                from.sendPacket(error.toString());
        }

    }

    private void transaction(int id, int amount) throws IOException {
        if (user.getAmount() < amount) {
            JSONObject packet = new JSONObject(packets.getJSONObject("error"));
            packet.put("message", "Not enough money");
            from.sendPacket(packet.toString());
            return;
        }
        for (ServerSession s : server.getClientList()) {
            if (s.getUser().getId() == id) {
                if (s.getCondition() == Condition.NORMAL) {
                    condition = Condition.WAITING_FOR_ACCEPT;

                    s.requestTransaction(this, amount);
                    return;
                } else {
                    JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
                    errorPacket.put("message", user.getLogin() + " is busy with another transaction");
                    from.sendPacket(errorPacket.toString());
                }
            }
        }
        JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
        errorPacket.put("message", "Wrong purse number or client is offline");
        from.sendPacket(errorPacket.toString());
    }

    public void requestTransaction(ServerSession from, int amount) {
        if (this.condition == Condition.NORMAL) {
            JSONObject requestPacket = new JSONObject(packets.getJSONObject("sendRequest"));
            requestPacket.put("sum", amount);
            requestPacket.put("sender", from.getUser().getLogin());
            requestPacket.put("recipient", this.getUser().getLogin());
            requestPacket.put("walletId", this.getUser().getId());
            this.sendPacket(requestPacket.toString());

            this.condition = Condition.ACCEPTING;
            this.from = from;
            this.amount = amount;
        }
        else {
            JSONObject errorPacket = new JSONObject(packets.getJSONObject("error"));
            errorPacket.put("message", user.getLogin() + " is busy with another transaction");
            from.sendPacket(errorPacket.toString());
        }

    }

    private void getList() throws IOException {
        FileReader fileReader = new FileReader(PATH_TO_CLIENTS_INFO);

        JSONObject walletListPacket = new JSONObject(packets.getJSONObject("walletList"));
        JSONArray walletArray = new JSONArray("wallets");
        JSONObject wallet;

        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                String[] line = str.split("\\s+");
                wallet = new JSONObject("wallet");
                wallet.put("owner", line[0]);
                wallet.put("uid", line[0]);
                walletArray.put(wallet);
            }
        }
        walletListPacket.put("wallets", walletArray);
        from.sendPacket(walletListPacket.toString());
    }

    private void getInfo() {
        JSONObject infoPacket = new JSONObject(packets.getJSONObject("answerWallet"));
        infoPacket.put("walletId", user.getId());
        infoPacket.put("sum", user.getAmount());
        from.sendPacket(infoPacket.toString());
    }

    private void signin(String login, String password) throws IOException {
        user = getUserInfo(login);
        if (user == null) {
            JSONObject packet = new JSONObject(packets.getJSONObject("error"));
            packet.put("message", ERR_SIGN);
            from.sendPacket(packet.toString());
            return;
        }
        if (!password.equals(user.getPassword())){
            JSONObject packet = new JSONObject(packets.getJSONObject("error"));
            packet.put("message", "Wrong password");
            from.sendPacket(packet.toString());
            return;
        }
        System.out.println(user.getLogin() + " is connected");
        JSONObject responsePacket = new JSONObject(packets.getJSONObject("response"));
        responsePacket.put("accepted", true);
        responsePacket.put("message", "Logged in");
        from.sendPacket(responsePacket.toString());
    }

    private void signup(String login, String password) throws IOException {
        user = new User(login, password);
        FileReader fileReader = new FileReader(PATH_TO_CLIENTS_INFO);
        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                String[] line = str.split("\\s+");
                if (line[0].equals(login)) {
                    JSONObject responsePacket = new JSONObject(packets.getJSONObject("response"));
                    responsePacket.put("accepted", false);
                    responsePacket.put("message", "This login is already used");
                    from.sendPacket(responsePacket.toString());
                    return;
                }
            }
            Utils.addLineInFile(user.getInfo() + "\n", PATH_TO_CLIENTS_INFO);
            System.out.println(user.getLogin() + " is connected");
            JSONObject responsePacket = new JSONObject(packets.getJSONObject("response"));
            responsePacket.put("accepted", true);
            responsePacket.put("message", "Signed up");
            from.sendPacket(responsePacket.toString());
        }
    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                din.close();
                dout.close();
                for (ServerSession vr : server.getClientList()) {
                    if (vr.equals(this))
                        server.removeClient(this);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private User getUserInfo(String login) throws IOException {
        FileReader fileReader = new FileReader(PATH_TO_CLIENTS_INFO);
        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] strings = line.split("\\s+");
                if (strings[0].equals(login)) {
                    return new User(strings);
                }
            }
            return null;
        }
    }

    private void disconnect() throws IOException {
        din.close();
        dout.close();
        socket.close();
        server.removeClient(this);
        connected = false;
    }

    public User getUser() {
        return user;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
