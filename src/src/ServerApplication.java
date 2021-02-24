import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerApplication {

    public static void main(String[] args) {
        Server server = new Server(parseArguments(args));
        try {
            server.run();
        } catch (IOException e) {
            System.out.println("Server terminated with unexpected exception");
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
    private int connectionsIdsCounter = 0;

    private int port;
    private int buffer;
    private String path;
    private static final Map<String, ServerConnection> connectionsList = new HashMap<>();

    public Server(String path) {
        this.path = path;
        this.init();
    }

    private void init() {
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
    }

    public void run() throws IOException {
        try (ServerSocket server = new ServerSocket(this.port)) {
            System.out.println("Server started successfully");
            while (server.isBound()) {
                Socket socket = server.accept();
                try {
                    connectionsList.put(String.valueOf(this.connectionsIdsCounter), new ServerConnection(socket, buffer, this.connectionsIdsCounter).init());
                    connectionsIdsCounter++;
                    System.out.println("New user joined!");
                } catch (IOException e) {
                    System.out.println("New user failed to join.");
                    socket.close();
                }
            }
        }
    }

    public static void sendToAllConnectedClients(byte[] bMsg) {
        for(ServerConnection serverConnection : connectionsList.values()) {
            try {
                serverConnection.getOut().write(bMsg);
                serverConnection.getOut().flush();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (StringIndexOutOfBoundsException ex) {
                ex.printStackTrace();
                System.out.println("incorrect message, please try again");
            }
        }
    }

    public static void removeClientById(String connectionId) {
        connectionsList.remove(connectionId);
    }

    public static Map<String, ServerConnection> getAllConnections() {
        return connectionsList;
    }

}

class ServerConnection extends Thread {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private String connectionId;
    private int buffer;

    public ServerConnection(Socket socket, int buffer, int connectionId) {
        this.socket = socket;
        this.buffer = buffer;
        this.connectionId = String.valueOf(connectionId);
    }

    public ServerConnection init() throws IOException {
        in = socket.getInputStream();
        out = socket.getOutputStream();
        this.start();
        return this;
    }

    @Override
    public void run() {
        byte[] word = new byte[this.buffer + 20];
        ArrayList<byte[]> parsedValues;
        try {
            while (true) {
                int i = in.read(word);
                word = inputCutter(word);
                parsedValues = inputParser(word);
                System.out.println("Server: <" + getTime(ByteConverterUtils.bToS(parsedValues.get(1)))
                        + "> [" + ByteConverterUtils.bToS(parsedValues.get(3)) + "] "
                        + ByteConverterUtils.bToS(parsedValues.get(5)) + "\n");
                Server.sendToAllConnectedClients(appendAll(parsedValues));
            }
        } catch (IOException e) {
            System.out.println("server connection is failed due to: " + e.getMessage());
        } finally {
            deinit();
        }
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    private String getTime(String millis) {
        Date time = new Date(Long.parseLong(millis));
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+3:00"));
        return sdf.format(time);
    }

    private String getCurrentTimeInMillis() {
        Date time = new Date();
        return String.valueOf(time.getTime());
    }


    private byte[] inputCutter(byte[] input) {
        if (input.length >= this.buffer) return Arrays.copyOfRange(input, 0, this.buffer);
        else return input;
    }

    private ArrayList<byte[]> inputParser(byte[] input) {
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

    private byte[] appendAll(ArrayList<byte[]> output) {
        byte[] temp1 = ByteConverterUtils.append(output.get(0), output.get(1));
        byte[] temp2 = ByteConverterUtils.append(temp1, output.get(2));
        byte[] temp3 = ByteConverterUtils.append(temp2, output.get(3));
        byte[] temp4 = ByteConverterUtils.append(temp3, output.get(4));
        return ByteConverterUtils.append(temp4, output.get(5));
    }

    private void deinit() {
        try {
            System.out.println("CLeaning up the user bound resources...");
            in.close();
            out.close();
            Server.removeClientById(this.connectionId);
            if (Server.getAllConnections().values().size() < 1) {
                System.out.println("no clients left... server is shutting down");
                socket.close();
            }
            System.out.println("removing client with id: " + connectionId);
            System.out.println("Clean up completed.");
        } catch (IOException e) {
            System.out.println("Clean up failed due to: " + e.getMessage());
        }
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
