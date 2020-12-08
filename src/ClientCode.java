import java.io.*;
import java.net.Socket;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class Main {
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
    private BufferedReader in;
    private BufferedWriter out;
    private BufferedReader userInp;
    private String name;
    private Socket socket;

    public ClientListener(String ip, int port, String userName) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            userInp = new BufferedReader(new InputStreamReader(System.in));
            name = userName;
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
                    char nameLength = (char) (name.length() + 30);
                    char userWordLength = (char) (userWord.length() + 30);
                    String output = nameLength + name + userWordLength + userWord;
                    out.write(output + "\n");
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
            String str;
            try {
                while (true) {
                    str = in.readLine();
                    if (str != null && !str.isEmpty()) {
                        consoleWriter(str);
                    }
                }
            } catch (IOException e) {
                System.out.println("Server disconnected. Disconnect clients");
            }
        }

        private void consoleWriter(String str) {
            String[] parsedInput = inputParser(str);
            System.out.println("<" + parsedInput[0] + "> [" + parsedInput[1] + "] " + parsedInput[2]);

        }

        private String[] inputParser(String input) {
            String[] result = new String[3];
            int timeLength = input.charAt(0) - 30;
            int nameLength = input.charAt(timeLength + 1) - 30;
            int messLength = input.charAt(timeLength + nameLength + 2) - 30;
            String time = timeConverter(input.substring(1, timeLength + 1));
            String name = input.substring(timeLength + 2, timeLength + nameLength + 2);
            String mess = input.substring(3 + timeLength + nameLength);
            result[0] = time;
            result[1] = name;
            result[2] = mess;
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




