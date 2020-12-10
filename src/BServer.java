import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


public class BServer {

    public static int PORT;
    public static int BUFFER;
    public static String PATH;
    public static LinkedList<ServerListener> serverList = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        argsParser(args);
        fileReader();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server starts successfully");
            while (server.isBound()) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerListener(socket));
                    System.out.println("socket was added");
                } catch (IOException e) {
                    System.out.println("socket was not created");
                    socket.close();
                }
            }
        }
    }

    private static void argsParser(String[] args) {
        if (args.length == 0) {
            PATH = "default";
            System.out.println("No path given. Program will use default values");
        } else if (args.length == 1) {
            PATH = args[0];
        } else {
            PATH = "default";
            System.out.println("Incorrect input given. Program will use default values");
        }
    }

    private static void fileReader() {
        if (PATH.equals("default")) {
            PORT = 8000;
            BUFFER = 1024;
        } else {
            try {
                File file = new File(PATH);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                PORT = Integer.parseInt(bufferedReader.readLine());
                BUFFER = Integer.parseInt(bufferedReader.readLine());
            } catch (IOException e) {
                PORT = 8000;
                BUFFER = 1024;
                System.out.println("Unable to read file. Program will use default values");
            }
        }
    }
}

class ServerListener extends Thread {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final B b = new B();

    public ServerListener(Socket socket) throws IOException {
        this.socket = socket;
        in = socket.getInputStream();
        out = socket.getOutputStream();
        start();
    }


    @Override
    public void run() {
        byte[] word = new byte[BServer.BUFFER + 20];
        ArrayList<byte[]> parsedValues;
        try {
            while (true) {
                int i = in.read(word);//
                word = inputCutter(word);//
                parsedValues = inputParser(word);//
                System.out.println("Server: <" + b.bToS(parsedValues.get(1)) + "> [" + b.bToS(parsedValues.get(3)) + "] " + b.bToS(parsedValues.get(5)) + "\n");
                for (ServerListener vr : BServer.serverList) {
                    vr.send(parsedValues);
                }
            }
        } catch (IOException e) {
            closer();
        }
    }


    private void send(ArrayList<byte[]> msg) {
        byte[] bMessage = outputGlue(msg);
        try {
            out.write(bMessage);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.out.println("incorrect message, please try again");
        }
    }


    private String getTime() {
        Date time = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(time);
    }


    private byte[] inputCutter(byte[] input) {
        if (input.length >= BServer.BUFFER) return Arrays.copyOfRange(input, 0, BServer.BUFFER);
        else return input;
    }


    private ArrayList<byte[]> inputParser(byte[] input) {
        ArrayList<byte[]> result = new ArrayList<>();
        int prePoint = 0;
        int postPoint = 4;

        String time = getTime();
        byte[] bTime = b.sToB(time);
        int timeLength = bTime.length;
        byte[] bTimeLength = b.iToB(timeLength);
        result.add(bTimeLength);
        result.add(bTime);

        byte[] bNameLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bNameLength);
        int bNL = bNameLength.length;
        int nameLength = b.bToI(bNameLength);
        prePoint = bNL;
        postPoint += nameLength;
        byte[] bName = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bName);

        prePoint = postPoint;
        postPoint += 4;
        byte[] bMessLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMessLength);
        int messLength = b.bToI(bMessLength);
        prePoint = postPoint;
        postPoint += messLength;
        byte[] bMess = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMess);

        return result;
    }

    private byte[] outputGlue(ArrayList<byte[]> output) {
        byte[] temp1 = b.sum(output.get(0), output.get(1));
        byte[] temp2 = b.sum(temp1, output.get(2));
        byte[] temp3 = b.sum(temp2, output.get(3));
        byte[] temp4 = b.sum(temp3, output.get(4));
        return b.sum(temp4, output.get(5));
    }

    private void closer() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


class B {

    public byte[] sToB(String str) {
        return str.getBytes(StandardCharsets.UTF_16);
    }

    public String bToS(byte[] b) {
        return new String(b, StandardCharsets.UTF_16);
    }

    public byte[] sum(byte[] a, byte[] b) {
        byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public int bToI(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public byte[] iToB(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public String arrToStr(byte[] b) {
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