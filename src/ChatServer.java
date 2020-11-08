import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private int port;
    private Set<String> userNames;
    private Set<UserThread> userThreads;

    public ChatServer(int port) {
        this.port = port;
        userNames = new HashSet<>();
        userThreads = new HashSet<>();
    }

    public void execute() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Chat server is listening on port "+ port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");

                UserThread newUser = new UserThread(socket, this);
                userThreads.add(newUser);
                newUser.start();
            }

        } catch (IOException e) {
            System.out.println("Error in the server : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length<1) {
            System.out.println("Syntax: java ChatServer port");
            return;
        }
        int port = Integer.parseInt(args[0]);

        ChatServer server = new ChatServer(port);
        server.execute();
    }

    public void broadcast(String message, UserThread excludeUser) {
        for (UserThread user : userThreads) {
            if (user!=excludeUser) {
                user.sendMessage(message);
            }
        }
    }

    public void addUserName(String userName) {
        userNames.add(userName);
    }

    public void removeUser(String userName, UserThread userThread) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(userThread);
            System.out.println("The user " + userName + " quited");
        }
    }

    public Set<String> getUserNames() {
        return this.userNames;
    }

    public boolean hasUsers() {
        return !this.userNames.isEmpty();
    }

}
