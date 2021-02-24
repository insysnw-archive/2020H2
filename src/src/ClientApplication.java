import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientApplication {
    private int port;
    private String ip;
    private String path;
    private String userName;

    public ClientApplication() {
        this.port = 8080;
        this.ip = "127.0.0.1";
        this.path = null;
        this.userName = "user_name";
    }

    public static void main(String[] args) {
        ClientApplication clientApplication = new ClientApplication();
        clientApplication.initFromArgs(args);
        clientApplication.initFromFile();
        ClientRunner clientRunner = new ClientRunner(clientApplication.ip, clientApplication.port, clientApplication.userName);
        clientRunner.run();
    }

    private void initFromArgs(String[] args) {
        if (args.length == 0) {
            System.out.println("Using default name and path");
        } else if (args.length == 1) {
            userName = args[0];
            System.out.println("Using default path");
        } else if (args.length == 2) {
            path = args[1];
            userName = args[0];
            System.out.println("Successfully set name and path");
        } else {
            System.out.println("Incorrect input. Program will use default values");
        }
        if (userName.length() > 12) {
            userName = userName.substring(0, 13);
        }
    }

    private void initFromFile() {
        if (path != null && !path.isEmpty()) {
            try {
                File file = new File(path);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                port = Integer.parseInt(bufferedReader.readLine());
                ip = bufferedReader.readLine();
                System.out.println("Config read successfully");
            } catch (Exception e) {
                System.out.println("Cannot find config file or it is incorrect. Program will use default values");
            }
        }
    }
}

class ClientRunner implements Runnable {
    private InputStream in;
    private OutputStream out;
    private BufferedReader userInput;
    private byte[] userName;
    private byte[] userNameLength;
    private Socket socket;

    public ClientRunner(String ip, int port, String userName) {
        try {
            this.socket = new Socket(ip, port);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.userInput = new BufferedReader(new InputStreamReader(System.in));
            this.userName = ByteConverterUtilsClient.sToB(userName);
            this.userNameLength = ByteConverterUtilsClient.iToB(this.userName.length);
        } catch (IOException e) {
            closer();
        }
    }

    private void closer() {
        try {
            in.close();
            out.close();
            userInput.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        new ReaderThreadExecutor().start();
        new WriterThreadExecutor().start();
    }

    private class WriterThreadExecutor extends Thread {
        @Override
        public void run() {
            while (true) {
                String userWord;
                try {
                    userWord = userInput.readLine();
                    if (userWord.length() > 255) userWord = userWord.substring(0, 100) + "...";
                    byte[] bUserWord = ByteConverterUtilsClient.sToB(userWord);
                    byte[] bUserWorldLength = ByteConverterUtilsClient.iToB(bUserWord.length);
                    byte[] temp1 = ByteConverterUtilsClient.sum(userNameLength, userName);
                    byte[] temp2 = ByteConverterUtilsClient.sum(temp1, bUserWorldLength);
                    byte[] output = ByteConverterUtilsClient.sum(temp2, bUserWord);
                    out.write(output);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReaderThreadExecutor extends Thread {
        @Override
        public void run() {
            byte[] bStr = new byte[65535];
            try {
                while (true) {
                    in.read(bStr);
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
            int timeLength = ByteConverterUtilsClient.bToI(bTimeLength);
            postPoint += timeLength;
            byte[] bTime = Arrays.copyOfRange(input, prePoint, postPoint);
            String time = ByteConverterUtilsClient.bToS(bTime);
            result.add(timeConverter(time));

            prePoint = postPoint;
            postPoint += 4;
            byte[] bNameLength = Arrays.copyOfRange(input, prePoint, postPoint);
            int nameLength = ByteConverterUtilsClient.bToI(bNameLength);
            prePoint = postPoint;
            postPoint += nameLength;
            byte[] bName = Arrays.copyOfRange(input, prePoint, postPoint);
            String name = ByteConverterUtilsClient.bToS(bName);
            result.add(name);

            prePoint = postPoint;
            postPoint += 4;
            byte[] bMessLength = Arrays.copyOfRange(input, prePoint, postPoint);
            int messLength = ByteConverterUtilsClient.bToI(bMessLength);
            prePoint = postPoint;
            postPoint += messLength;
            byte[] bMess = Arrays.copyOfRange(input, prePoint, postPoint);
            String mess = ByteConverterUtilsClient.bToS(bMess);
            result.add(mess);
            return result;
        }

        private String getTime(String millis) {
            Date time = new Date(Long.parseLong(millis));
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+3:00"));
            return sdf.format(time);
        }

        private String timeConverter(String millis) {
            String GMTTime = getTime(millis);
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

class ByteConverterUtilsClient {

    public static byte[] sToB(String str) {
        return str.getBytes(StandardCharsets.UTF_16);
    }

    public static String bToS(byte[] b) {
        return new String(b, StandardCharsets.UTF_16);
    }

    public static byte[] sum(byte[] a, byte[] b) {
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
