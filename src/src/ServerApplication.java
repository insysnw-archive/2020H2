import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerApplication {

    public static void main(String[] args) {
        Server server = new Server(parseArguments(args));
        try {
            server.init();
            server.run();
        } catch (IOException e) {
            System.out.println("Server terminated with unexpected exception: " + e.getMessage());
        }
    }

    private static String parseArguments(String[] args) {
        if (args.length == 0) {
            System.out.println("No path given. Program will use default values");
        } else if (args.length == 1) {
            return args[0];
        } else {
            System.out.println("Incorrect input given. Program will use default values");
        }
        return null;
    }
}

class Server {
    private int DEFAULT_PORT = 8080;
    private int DEFAULT_BUFFER = 1024;

    private int port;
    private int buffer;
    private String path;

    private InetAddress host;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private SelectionKey selectionKey;
    private ArrayList<SocketChannel> socketChannels;

    public Server(String path) {
        this.path = path;
    }

    public void init() throws IOException {
        if (this.path == null) {
            this.buffer = DEFAULT_BUFFER;
            this.port = DEFAULT_PORT;
        } else {
            try {
                File configFile = new File(this.path);
                FileReader fileReader = new FileReader(configFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                this.port = Integer.parseInt(bufferedReader.readLine());
                this.buffer = Integer.parseInt(bufferedReader.readLine());
            } catch (Exception e) {
                System.out.println("Unable to read file. Program will use default values");
                this.buffer = DEFAULT_BUFFER;
                this.port = DEFAULT_PORT;
            }
        }
        this.configure();
    }

    private void configure() throws IOException {
        this.host = InetAddress.getByName("localhost");
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(host, this.port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        socketChannels = new ArrayList<>();
    }

    public void run() throws IOException {
        while (true) {
            if (selector.select() <= 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                selectionKey = iterator.next();
                iterator.remove();
                if (selectionKey.isAcceptable()) {
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.
                            OP_READ);
                    System.out.println("Connection Accepted: "
                            + sc.getLocalAddress() + "\n");
                    socketChannels.add(sc);
                }
                if (selectionKey.isReadable()) {
                    SocketChannel sc = (SocketChannel) selectionKey.channel();
                    ByteBuffer bb = ByteBuffer.allocate(DEFAULT_BUFFER);
                    try {
                        sc.read(bb);
                    } catch (Exception e) {
                        System.out.println("Failed to read from: " + sc.socket().getInetAddress().toString()
                                + ":"+ sc.socket().getPort() + "\n");
                        sc.close();
                        socketChannels.remove(sc);
                        System.out.println("This client is disconnected." + "\n");
                        continue;
                    }
                    ArrayList<byte[]> parsedValues = ParserUtils.inputParser(bb.array());
                    String result = new String(bb.array()).trim();
                    System.out.println("Server: <" + ParserUtils.getTime(ByteConverterUtils.bToS(parsedValues.get(1)))
                            + "> [" + ByteConverterUtils.bToS(parsedValues.get(3)) + "] "
                            + ByteConverterUtils.bToS(parsedValues.get(5)) + "\n");
                    byte[] glue = ParserUtils.appendAll(parsedValues);
                    ByteBuffer b1 = ByteBuffer.wrap(glue);

                    for(SocketChannel c : socketChannels){
                        c.configureBlocking(false);
                        c.write(b1);
                        b1.flip();
                    }
                }

            }
        }
    }
}


class ParserUtils {
    public static byte[] appendAll(ArrayList<byte[]> output) {
        byte[] temp1 = ByteConverterUtils.append(output.get(0), output.get(1));
        byte[] temp2 = ByteConverterUtils.append(temp1, output.get(2));
        byte[] temp3 = ByteConverterUtils.append(temp2, output.get(3));
        byte[] temp4 = ByteConverterUtils.append(temp3, output.get(4));
        return ByteConverterUtils.append(temp4, output.get(5));
    }

    public static ArrayList<byte[]> inputParser(byte[] input) {
        ArrayList<byte[]> result = new ArrayList<>();
        int prePoint = 0;
        int postPoint = 4;

        String time = getCurrentTimeInMillis();
        byte[] bTime = ByteConverterUtils.sToB(time);
        int timeLength = bTime.length;
        byte[] bTimeLength = ByteConverterUtils.iToB(timeLength);
        result.add(bTimeLength);
        result.add(bTime);

        byte[] bNameLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bNameLength);
        int bNL = bNameLength.length;
        int nameLength = ByteConverterUtils.bToI(bNameLength);
        prePoint = bNL;
        postPoint += nameLength;
        byte[] bName = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bName);

        prePoint = postPoint;
        postPoint += 4;
        byte[] bMessLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMessLength);
        int messLength = ByteConverterUtils.bToI(bMessLength);
        prePoint = postPoint;
        postPoint += messLength;
        byte[] bMess = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMess);

        return result;
    }

    public static String getTime(String millis) {
        Date time = new Date(Long.parseLong(millis));
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+3:00"));
        return sdf.format(time);
    }

    private static String getCurrentTimeInMillis() {
        Date time = new Date();
        return String.valueOf(time.getTime());
    }
}

class ByteConverterUtils {
    public static byte[] sToB(String str) {
        return str.getBytes(StandardCharsets.UTF_16);
    }

    public static String bToS(byte[] b) {
        return new String(b, StandardCharsets.UTF_16);
    }

    public static byte[] append(byte[] a, byte[] b) {
        byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public static int bToI(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public static byte[] iToB(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public static String arrToStr(byte[] b) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            res.append(b[i]);
            if (i != b.length - 1) {
                res.append(", ");
            }
        }
        return res.toString();
    }
}
