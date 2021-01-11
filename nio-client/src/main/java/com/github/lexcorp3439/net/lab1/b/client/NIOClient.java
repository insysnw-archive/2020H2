package com.github.lexcorp3439.net.lab1.b.client;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.github.lexcorp3439.net.lab1.protocol.Protocol;
import com.github.lexcorp3439.net.lab1.protocol.ProtocolHelper;

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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ProtocolHelper.write(ProtocolHelper.build(username, "quit"), socket);
        }));
    }

    public void connect() {
        try {
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket = SocketChannel.open(address);
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Не удалось подключиться к серверу");
            System.exit(1);
        }
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
            if ("quit".equals(line)) {
                System.out.println("Goodbye!!!");
                System.exit(0);
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

                String msgText = ProtocolHelper.toLine(msgs);

                String messageText = String.format(
                        "<%s:%s> [%s] %s",
                        calendar.get(Calendar.HOUR),
                        calendar.get(Calendar.MINUTE),
                        msgs.get(0).getName(),
                        msgText
                );

                System.out.println(messageText);

                if (msgText.equals("This username already exist. Reconnect with other username!")) {
                    System.exit(1);
                }
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
        String username = ProtocolHelper.generateUsername(10);

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
