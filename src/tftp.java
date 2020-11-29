import java.net.*;
import java.io.*;

public class tftp {

    private static int port = 69;
    private static String address = "127.0.0.1";
    private static final String HELP_ARG = "-h";
    private static final String HOST_ARG = "--host";
    private static final String PORT_ARG = "-p";

    public static void main(String[] args) {

        if (args.length == 0) {
            startServer();
        } else if (args.length == 1 && args[0].equals(HELP_ARG)) {
            printUsage();
        } else if (args.length == 2 && args[0].equals(HOST_ARG)) {
            address = args[1];
            startServer();
        } else if (args.length == 3 && args[0].equals(PORT_ARG)) {
            port = Integer.parseInt(args[1]);
            startServer();
        } else if (args.length == 4 && args[0].equals(HOST_ARG) && args[2].equals(PORT_ARG)) {
            address = args[1];
            port = Integer.parseInt(args[3]);
            startServer();
        } else if (args.length == 4 && args[0].equals(PORT_ARG) && args[2].equals(HOST_ARG)) {
            address = args[3];
            port = Integer.parseInt(args[1]);
            startServer();
        } else {
            printUsage();
        }
    }

    private static void startServer() {
        try {
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(address));
            System.out.println("Server Ready. Address: " + address + " Port: " + port);

            while (true) {
                TftpPacket in = TftpPacket.receive(socket);
                if (in instanceof ReadPacket) {
                    System.out.println("Read Request from " + in.getAddress());
                    ReadRequest r = new ReadRequest((ReadPacket) in);
                }
                else if (in instanceof WritePacket) {
                    System.out.println("Write Request from " + in.getAddress());
                    WriteRequest w = new WriteRequest((WritePacket) in);
                }
            }
        } catch (SocketException e) {
            System.out.println("Server terminated(SocketException) " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Server terminated(IOException)" + e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("Usage: [-h] [--host HOST] [-p PORT]\n" +
                "optional arguments:\n" +
                "  -h, --help            show this help message and exit\n" +
                "  --host HOST           IP of the interface the server will listen on.\n" +
                "                        Default: 127.0.0.1\n" +
                "  -p PORT, --port PORT  Port the server will listen on. Default: 69. TFTP\n" +
                "                        standard-compliant port: 69 - requires superuser\n" +
                "                        privileges.");
    }
}