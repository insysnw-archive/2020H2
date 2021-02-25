package com.alexandr.server;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    int first_appear = 0;
    private OutputStream outMessage = null;
    private InputStream inMessage = null;
private ByteArrayInputStream inp;
    private Scanner scan;
    Socket socket = null;
    String my_name = "";

    Server server;
    ClientHandler(Socket clsock, Server serv) throws IOException {
        this.socket = clsock;
        this.inMessage = socket.getInputStream();
        this.outMessage = socket.getOutputStream();
        this.scan = new Scanner(socket.getInputStream());
       // writer = new PrintWriter(outMessage);
        this.server = serv;
        ClientMessege cm = new ClientMessege();
        byte[] data = new byte[2048];
        int count = clsock.getInputStream().read(data);
        cm.decodeMsg(data);
        my_name = cm.getName();
    }

    @Override
    public void run() {
        while (true){

            if(scan.hasNext()){
                if(first_appear == 0){
                    try {
                        byte[] data = new byte[2048];
                        int count = socket.getInputStream().read(data);
                            server.sendToAll(2, data);
                            first_appear = 1;
                    } catch (IOException exception) {
                        exception.printStackTrace();
                            System.exit(-3);
                    }
                } else {
                    try {
                        byte[] data = new byte[2048];
                        int count = socket.getInputStream().read(data);
                        ClientMessege cm = new ClientMessege();
                        cm.decodeMsg(data);
                        if(cm.getText().equals("close chat")){
                            server.sendToAll(3,data);
                            socket.close();
                            scan.close();
                            inMessage.close();
                            outMessage.close();
                            server.removeClient(this);
                            server.names.remove(my_name);
                            break;
                        } else {
                            server.sendToAll(0,data);
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() throws IOException {
        byte[] m = new byte[10];
        server.sendToAll(1,m);
        socket.close();

    }


    public void sendMsg(byte[] msg){
        try {
            socket.getOutputStream().write(msg);
            ClientMessege cm = new ClientMessege();
            cm.decodeMsg(msg);
            System.out.println("Новое сообщение: "+cm.getTime()+ " "+ cm.getName()+ " "+ cm.getText());
            socket.getOutputStream().flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
