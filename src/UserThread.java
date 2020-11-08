import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserThread extends Thread{
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter writer;

    public UserThread(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {

            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            OutputStream outputStream = socket.getOutputStream();
            writer = new PrintWriter(outputStream, true);

//            printUsers();

            String userName = reader.readLine();
            server.addUserName(userName);

            String serverMessage = "New user connected: " + userName;
            server.broadcast(serverMessage, this);

            String clientMessages;

            do {
                clientMessages = reader.readLine();
                serverMessage = "["+userName+"]: " + clientMessages;
                server.broadcast(serverMessage, this);
                System.out.println(getMessageDescription(userName)+clientMessages);
            } while (!clientMessages.equals("bye"));

            server.removeUser(userName, this);
            socket.close();
            serverMessage = userName + " has quited.";
            server.broadcast(serverMessage, this);

        } catch (IOException e) {
            System.out.println("Error in UserThread : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getMessageDescription(String userName) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<"+dateTimeFormatter.format(now)+">"+"["+userName+"]: ";
    }

//    private void printUsers() {
//        if (server.hasUsers()) {
//            writer.println("Connected users: " + server.getUserNames());
//        } else {
//            writer.println("No other users connected");
//        }
//    }

    public void sendMessage(String message) {
        writer.println(message);
    }
}
