import java.io.Console;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Client {

    public void execute(String host, int port) {
        try {
            Console console = System.console();
            String clientName = console.readLine("\nEnter your name: ");
            Socket socket = new Socket(host, port);
            new RWThread(socket, clientName).start();
        } catch (SocketException connectException) {
            System.out.println(connectException.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        if (args.length == 1) {
            client.execute(args[0], Strings.DEFAULT_PORT);
        } else if (args.length > 1) {
            client.execute(args[0], Integer.parseInt(args[1]));
        } else client.execute(Strings.DEFAULT_ADDRESS, Strings.DEFAULT_PORT);
    }
}
