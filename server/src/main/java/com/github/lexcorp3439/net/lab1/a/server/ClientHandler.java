package com.github.lexcorp3439.net.lab1.a.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import com.github.lexcorp3439.net.lab1.protocol.Protocol;
import com.github.lexcorp3439.net.lab1.protocol.ProtocolHelper;

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

            while (clientDialog.isConnected()) {
                List<Protocol> msgs = ProtocolHelper.read(in);

                if (username == null) {
                    this.username = msgs.get(0).getName();

                    if (
                            !"SERVER".equals(this.username) &&
                                    listHandlers.stream().filter(it -> this.username.equals(it.username)).count() > 1
                    ) {
                        this.write(
                                ProtocolHelper.build("SERVER", "This username already exist. Reconnect with other username!")
                        );
                        break;
                    } else {
                        System.out.println("Connection accepted. User " + username);
                    }
                }

                String message = ProtocolHelper.toLine(msgs);
                System.out.println("[" + username + "] " + message);

                if ("quit".equals(message) || message.isEmpty()) {
                    System.out.println("Client[" + username + "] initialize connections suicide ...");
                    break;
                }

                for (ClientHandler handler : listHandlers) {
                    if (!this.username.equals(handler.username)) {
                        handler.write(ProtocolHelper.build(username, message));
                    }
                }
            }

            in.close();
            out.close();

            clientDialog.close();
            listHandlers.removeIf(it -> it.username.equals(this.username));

            System.out.println("Client[" + username + "] disconnected");
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
