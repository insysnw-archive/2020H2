package lab1.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executeIt = Executors.newFixedThreadPool(2);
    static final String SERVER_NAME = "server";
    private final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
    private final int port;
    private final InetAddress address;

    public Server() {
        this(null, 3345);
    }

    public Server(InetAddress address, int port) {
        this.port = port;
        this.address = address;
    }

    public void start() {
        try (
                ServerSocket server = new ServerSocket(port, 50, address);
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
        Server server = createServer(args);
        server.start();
    }

    private static Server createServer(String[] args) {
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iterator = argsList.listIterator();
        int port = 3345;
        InetAddress address = null;

        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-p":
                    port = Integer.parseInt(iterator.next());
                    break;
                case "-h":
                    try {
                        address = Inet4Address.getByName(iterator.next());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    break;
                case "-a":
                    try {
                        address = Inet4Address.getByAddress(iterator.next().getBytes());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        return new Server(address, port);
    }
}