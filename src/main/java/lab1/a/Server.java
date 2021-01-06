package lab1.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private final ExecutorService executeIt = Executors.newFixedThreadPool(2);
    static final String SERVER_NAME = "server";
    private final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    private final int port;

    public Server() {
        this(3345);
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (
                ServerSocket server = new ServerSocket(port);
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Server start");
            while (!server.isClosed()) {
                if (br.ready()) {
                    System.out.println("Main Server found any messages in channel, let's look at them.");
                    String serverCommand = br.readLine();
                    if (serverCommand.equalsIgnoreCase("quit")) {
                        System.out.println("Main Server initiate exiting...");
                        server.close();
                        break;
                    }
                }

                Socket client = server.accept();

                ClientHandler clientHandler = new ClientHandler(client, handlers);
                handlers.add(clientHandler);
                executeIt.execute(clientHandler);
            }

            executeIt.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}