import java.io.Console;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatClient{
    private String hostName;
    private int port;
    private String userName;

    public ChatClient(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void execute() {
        try {

            Console console = System.console();
            String userName = console.readLine("\nEnter your name: ");
            setUserName(userName);

            Socket socket = new Socket(hostName, port);
            System.out.println("Connected to the chat server");


            new WriteThread(socket, this, userName).start();
            new ReadThread(socket,this).start();


        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    public static void main(String[] args) {
        if (args.length<2) return;
        String hostName = args[0];
        int port = Integer.parseInt(args[1]);

        ChatClient client = new ChatClient(hostName, port);
        client.execute();
    }
}
