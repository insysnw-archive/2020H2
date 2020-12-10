import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class BClient {
    public static int PORT;
    public static String IP;
    public static String PATH;
    public static String NAME;

    public static void main(String[] args) {
        argsParser(args);
        fileReader();
        new ClientListener(IP, PORT, NAME);
    }

    private static void argsParser(String[] args) {
        if (args.length == 0) {
            PATH = "default";
            NAME = "Anon";
            System.out.println("Using default name and path");
        } else if (args.length == 1) {
            PATH = "default";
            NAME = args[0];
            System.out.println("Using default path");
        } else if (args.length == 2) {
            PATH = args[1];
            NAME = args[0];
            System.out.println("Successfully set name and path");
        } else {
            System.out.println("Incorrect input. Program will use default values");
        }
        if (NAME.length() > 12) {
            NAME = NAME.substring(0, 13);
        }
    }

    private static void fileReader() {
        if (!PATH.equals("default")) {
            try {
                File file = new File(PATH);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                PORT = Integer.parseInt(bufferedReader.readLine());
                IP = bufferedReader.readLine();
                System.out.println("Config read successfully");
            } catch (Exception e) {
                System.out.println("Cannot find config file or it is incorrect. Program will use default values");
                PORT = 8000;
                IP = "127.0.0.1";
            }
        } else {
            PORT = 8000;
            IP = "127.0.0.1";
            System.out.println("Given default path. Program will use default values");
        }
    }

}

class ClientListener {
    private InputStream in;
    private OutputStream out;
    private BufferedReader userInp;
    private String name;
    private byte[] bName;
    private byte[] bNameLength;
    private Socket socket;
    private final B b = new B();

    public ClientListener(String ip, int port, String userName) {
        try {
            socket = new Socket(ip, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            userInp = new BufferedReader(new InputStreamReader(System.in));
            name = userName;
            bName = b.sToB(name);
            int nameLength = bName.length;
            bNameLength = b.iToB(nameLength);
            new ReadMsg().start();
            new WriteMsg().start();
        } catch (IOException e) {
            closer();
        }
    }

    private void closer() {
        try {
            in.close();
            out.close();
            userInp.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class WriteMsg extends Thread {
        @Override
        public void run() {
            while (true) {
                String userWord;
                try {
                    userWord = userInp.readLine();
                    if (userWord.length() > 255) userWord = userWord.substring(0, 100) + "...";
                    byte[] bUserWord = b.sToB(userWord);
                    byte[] bUserWorldLength = b.iToB(bUserWord.length);
                    byte[] temp1 = b.sum(bNameLength, bName);
                    byte[] temp2 = b.sum(temp1, bUserWorldLength);
                    byte[] output = b.sum(temp2, bUserWord);
                    out.write(output);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class ReadMsg extends Thread {

        @Override
        public void run() {
            byte[] bStr = new byte[65535];
            try {
                while (true) {
                    int i = in.read(bStr);
                    consoleWriter(bStr);
                }
            } catch (IOException e) {
                System.out.println("Server disconnected. Disconnect clients");
            }
        }

        private void consoleWriter(byte[] str) {
            ArrayList<String> parsedInput = inputParser(str);
            System.out.println("<" + parsedInput.get(0) + "> [" + parsedInput.get(1) + "] " + parsedInput.get(2));

        }

        private ArrayList<String> inputParser(byte[] input) {
            ArrayList<String> result = new ArrayList<>();
            int prePoint = 0;
            int postPoint = 4;

            byte[] bTimeLength = Arrays.copyOfRange(input, prePoint, postPoint);
            prePoint = postPoint;
            int timeLength = b.bToI(bTimeLength);
            postPoint += timeLength;
            byte[] bTime = Arrays.copyOfRange(input, prePoint, postPoint);
            String time = b.bToS(bTime);
            result.add(timeConverter(time));

            prePoint = postPoint;
            postPoint += 4;
            byte[] bNameLength = Arrays.copyOfRange(input, prePoint, postPoint);
            int nameLength = b.bToI(bNameLength);
            prePoint = postPoint;
            postPoint += nameLength;
            byte[] bName = Arrays.copyOfRange(input, prePoint, postPoint);
            String name = b.bToS(bName);
            result.add(name);

            prePoint = postPoint;
            postPoint += 4;
            byte[] bMessLength = Arrays.copyOfRange(input, prePoint, postPoint);
            int messLength = b.bToI(bMessLength);
            prePoint = postPoint;
            postPoint += messLength;
            byte[] bMess = Arrays.copyOfRange(input, prePoint, postPoint);
            String mess = b.bToS(bMess);
            result.add(mess);
            return result;
        }

        private String timeConverter(String GMTTime) {
            Calendar calendar = new GregorianCalendar();
            TimeZone mTimeZone = calendar.getTimeZone();
            int gmtRawOffset = mTimeZone.getRawOffset();
            int offset = (int) TimeUnit.HOURS.convert(gmtRawOffset, TimeUnit.MILLISECONDS);
            String hoursString = GMTTime.substring(0, 2);
            int hours = Integer.parseInt(hoursString);
            if (hours + offset > 23 && offset > 0) {
                hours = hours - 24;
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            } else if (hours + offset < 0 && offset < 0) {
                hours = hours + 24;
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            } else {
                int offsetTime = hours + offset;
                return offsetTime + GMTTime.substring(2);
            }
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
        String res = "";
        for (int i = 0; i < b.length; i++) {
            res += b[i];
            if (i != b.length - 1) {
                res += ", ";
            }
        }
        return res;
    }

}
