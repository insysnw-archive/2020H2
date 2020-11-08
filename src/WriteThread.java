import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatClient chatClient;
    private String userName;

    public WriteThread(Socket socket, ChatClient chatClient, String userName) {
        this.socket = socket;
        this.chatClient = chatClient;
        this.userName = userName;
        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.out.println("Error getting output stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void run() {
        Console console = System.console();
//        String userName = console.readLine("\nEnter your name: ");
//        chatClient.setUserName(userName);
        writer.println(userName);

        String text;

        do {
//            text = console.readLine(getMessageDescription(userName));
            text = console.readLine();
            writer.println(text);
        } while (!text.equals("bye"));

        try {
            socket.close();
        } catch (IOException ex) {

            System.out.println("Error writing to server: " + ex.getMessage());
        }
    }

    private String getMessageDescription(String userName) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<"+dateTimeFormatter.format(now)+">"+"["+userName+"]: ";
    }
}
