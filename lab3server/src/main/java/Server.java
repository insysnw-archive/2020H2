import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private final ServerSocket serverSocket;
    static final Map<String, ClientSocketThread> clients = new HashMap<>();
    static final Map<String, Lot> lots = new HashMap<>();
    public static JSONObject packets;
    public static final int PORT = 3333;
    private boolean working = true;

    public Server(int port) throws IOException {
        packets = new JSONObject(new JSONTokener(Server.class.getResourceAsStream("/packets.json")));
        serverSocket = new ServerSocket(port);
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
            Socket socket = serverSocket.accept();
            ClientSocketThread socketThread = new ClientSocketThread(socket);
            clients.put(socket.getInetAddress().toString() + socket.getPort(), socketThread);
            socketThread.start();
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
                            for(ClientSocketThread client: clients.values()) {
                                System.out.println(client.getClient().toString());
                            }
                            break;
                        }
                        case "disconnect": {
                            System.out.println("Enter client's username: ");
                            String name = br.readLine();
                            for(ClientSocketThread client: clients.values()) {
                                if(client.getClient().getUsername().equals(name)) {
                                    disconnectClient(client.getSocket().getInetAddress(), client.getSocket().getPort());
                                }
                            }
                            break;
                        }
                        case "quit": {
                            for (ClientSocketThread client: clients.values()) {
                                client.disconnectClient();
                            }
                            serverSocket.close();
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

    private synchronized void disconnectClient(InetAddress address, int port) throws IOException {
        if(clients.containsKey(address.toString() + port))
            clients.get(address.toString() + port).disconnectClient();
        else
            System.out.println("Client doesn't exist");
    }
}