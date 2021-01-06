package lab1.a;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import lab1.model.Protocol;
import lab1.model.ProtocolHelper;

public class ClientHandler implements Runnable {

    private static Socket clientDialog;
    private String username = null;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final List<ClientHandler> listHandlers;

    public ClientHandler(Socket client, List<ClientHandler> otherHandlers) {
        ClientHandler.clientDialog = client;
        this.listHandlers = otherHandlers;
    }

    // <4:20> [Kenobi] Hello there!
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientDialog.getOutputStream());
            in = new ObjectInputStream(clientDialog.getInputStream());

            while (!clientDialog.isClosed()) {
                List<Protocol> msgs = ProtocolHelper.read(in);

                if (username == null) {
                    this.username = msgs.get(0).getName();
                    System.out.println("Connection accepted. User " + username);
                }

                String message = ProtocolHelper.toLine(msgs);

                if ("quit".equals(message)) {
                    System.out.println("Client initialize connections suicide ...");
                    break;
                }

                for (ClientHandler handler : listHandlers) {
                    if (!this.username.equals(handler.username)) {
                        handler.write(ProtocolHelper.build(username, message));
                    }
                }
            }

            System.out.println("Client disconnected");
            System.out.println("Closing connections & channels.");

            in.close();
            out.close();

            clientDialog.close();

            System.out.println("Closing connections & channels - DONE.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(byte[] data) {
        try {
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(List<Protocol> msgs) {
        try {
            ProtocolHelper.write(msgs, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
