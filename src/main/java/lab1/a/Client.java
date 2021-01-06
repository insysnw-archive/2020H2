package lab1.a;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;

import lab1.model.Protocol;
import lab1.model.ProtocolHelper;

import static lab1.Utils.randomString;

public class Client {
    static Socket socket;
    private final String username;
    private final int port;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public Client() {
        this("localhost", 3345, "lexcorp");
    }

    public Client(String host, int port) {
        this(host, port, "lexcorp");
    }

    public Client(String username) {
        this("localhost",3345, username);
    }

    public Client(String host, int port, String username) {
        this.username = username;
        this.port = port;
        try {
            // создаём сокет общения на стороне клиента в конструкторе объекта
            socket = new Socket(host, port);
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            MessageReader reader = new MessageReader();
            reader.setDaemon(true);
            reader.start();

            System.out.println("Connection accepted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()) {
            String line = scanner.nextLine();
            if (!line.isEmpty()) {
                try {
                    ProtocolHelper.write(ProtocolHelper.build(username, line), oos);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MessageReader extends Thread {
        @Override
        public void run() {
            while (socket.isConnected()) {
                List<Protocol> msgs = ProtocolHelper.read(ois);
                assert !msgs.isEmpty();

                Calendar calendar = new GregorianCalendar();

                String messageText = String.format(
                        "<%s:%s> [%s] %s",
                        calendar.get(Calendar.HOUR),
                        calendar.get(Calendar.MINUTE),
                        username,
                        ProtocolHelper.toLine(msgs)
                );

                System.out.println(messageText);
            }
        }
    }


    public static void main(String[] args) {
        Client client = createClient(args);
        client.connect();
        client.listen();
    }

    private static Client createClient(String[] args) {
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
        return new Client(host, port, username);
    }

}
