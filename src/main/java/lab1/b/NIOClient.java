package lab1.b;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import lab1.model.Protocol;
import lab1.model.ProtocolHelper;

import static lab1.Utils.randomString;

public class NIOClient {
    static SocketChannel socket;
    private final String username;
    private final int port;
    private final String host;

    public NIOClient() {
        this("localhost", 3345, "lexcorp");
    }

    public NIOClient(String host, int port) {
        this(host, port, "lexcorp");
    }

    public NIOClient(String username) {
        this("localhost", 3345, username);
    }

    public NIOClient(String host, int port, String username) {
        this.username = username;
        this.port = port;
        this.host = host;
        try {
            // создаём сокет общения на стороне клиента в конструкторе объекта
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket = SocketChannel.open(address);
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        MessageReader reader = new MessageReader();
        reader.setDaemon(true);
        reader.start();
        System.out.println("Connection accepted.");
    }

    public void listen() {
        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()) {
            String line = scanner.nextLine();
            if (!line.isEmpty()) {
                ProtocolHelper.write(ProtocolHelper.build(username, line), socket);
            }
        }
    }

    class MessageReader extends Thread {
        @Override
        public void run() {
            while (socket.isConnected()) {
                List<Protocol> msgs = ProtocolHelper.read(socket);
                assert !msgs.isEmpty();

                Calendar calendar = new GregorianCalendar();

                String messageText = String.format(
                        "<%s:%s> [%s] %s",
                        calendar.get(Calendar.HOUR),
                        calendar.get(Calendar.MINUTE),
                        msgs.get(0).getName(),
                        ProtocolHelper.toLine(msgs)
                );

                System.out.println(messageText);
            }
        }
    }

    public static void main(String[] args) {
        NIOClient client = createClient(args);
        client.connect();
        client.listen();
    }

    private static NIOClient createClient(String[] args) {
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iterator = argsList.listIterator();
        int port = 3345;
        String host = "localhost";
        String username = randomString(10);

        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-p":
                    port = Integer.parseInt(iterator.next());
                    break;
                case "-h":
                    host = iterator.next();
                    break;
                case "-u":
                    username = iterator.next();
                    break;
            }
        }
        return new NIOClient(host, port, username);
    }
}
