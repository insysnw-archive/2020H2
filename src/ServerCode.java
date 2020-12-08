import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;


public class Main {

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
            BUFFER = 255;
        } else {
            try {
                File file = new File(PATH);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                PORT = Integer.parseInt(bufferedReader.readLine());
                BUFFER = Integer.parseInt(bufferedReader.readLine());
            } catch (IOException e) {
                PORT = 8000;
                BUFFER = 255;
                System.out.println("Unable to read file. Program will use default values");
            }
        }
    }
}

class ServerListener extends Thread {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    public ServerListener(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
    }

    @Override
    public void run() {
        String word;
        String[] parsedValues;
        try {
            while (true) {
                word = in.readLine();
                word = inputCutter(word);
                parsedValues = inputParser(word);
                System.out.println("Server: <" + parsedValues[0] + "> [" + parsedValues[1] + "] " + parsedValues[2] + "\n");
                for (ServerListener vr : Main.serverList) {
                    vr.send(parsedValues);
                }
            }
        } catch (IOException e) {
            closer();
        }
    }


    private void send(String[] msg) {
        String message = outputGlue(msg);
        try {
            out.write(message + "\n");
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

    private String inputCutter(String input) {
        if (input.length() >= Main.BUFFER) return input.substring(0, Main.BUFFER - 4) + "...";
        else return input;
    }

    private String[] inputParser(String input) {
        String[] result = new String[3];
        int nameLength = input.charAt(0) - 30;
        String name = input.substring(1, nameLength + 1);
        int msgLength = input.charAt(nameLength + 1) - 30;
        String message = input.substring(2 + nameLength);
        String time = getTime();
        result[0] = time;
        result[1] = name;
        result[2] = message;
        return result;
    }

    private String outputGlue(String[] output) {
        String result;
        int timeLength = output[0].length() + 30;
        int nameLength = output[1].length() + 30;
        int messLength = output[2].length() + 30;
        result = (char) timeLength + output[0] + (char) nameLength + output[1] + (char) messLength + output[2];
        return result;
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