package com.alexandr.server;


import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {
    Socket clientSocket = null;
    ServerSocket serverSocket = null;
    Integer port;
    String host;
    boolean work = true;
    List<ClientHandler> clientsList = new ArrayList<>();
    List<String> names = new ArrayList<>();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    Server(String host, Integer port){
        this.port = port;
        this.host = host;
    }

    private void disconnect(){
        sendToAll(1,null);
        System.out.println("Сервер выключен");
        System.exit(0);
    }

    public void removeClient(ClientHandler client){
        clientsList.remove(client);
    }

    public void sendToAll(int flag,byte[] mesg){
        switch (flag) {

            case 0 -> {
                ClientMessege pack = new ClientMessege();
                pack.decodeMsg(mesg);
                byte[] msg = pack.createArr(0);
                if(clientsList.size() !=0){
                    for (ClientHandler ch : clientsList) {
                        ch.sendMsg(msg);
                    }
                }
            }
            case 1 -> { // отключение сервера
                ClientMessege pack = new ClientMessege();
                pack.setTime(dtf.format(LocalTime.now()));
                byte[] msg = pack.createArr(1);
                System.out.println("[SERVER] Сервер отключается");
                if(clientsList.size() !=0){
                    for (ClientHandler ch : clientsList) {
                        ch.sendMsg(msg);
                    }
                }
            }

            case 2 -> { // клиент вошел в чат
                ClientMessege pack = new ClientMessege();
                pack.decodeMsg(mesg);
                names.add(pack.getName());
                System.out.println("[SERVER] Клиент "+ pack.getName() + " вошел в чат");
                byte[] msg = pack.createArr(2);
                if(clientsList.size() !=0){
                    for (ClientHandler ch : clientsList) {
                        ch.sendMsg(msg);
                    }
                }
            }
            case 3 -> { // клиент покинул чат
                ClientMessege pack = new ClientMessege();
                pack.decodeMsg(mesg);
                names.remove(pack.getName());
                System.out.println("[SERVER] Клиент покинул чат");
                byte[] msg = pack.createArr(3);
                if(clientsList.size() !=0){
                    for (ClientHandler ch : clientsList) {
                        ch.sendMsg(msg);
                    }
                }
            }
            case 4 -> { // дублирование
                ClientMessege pack = new ClientMessege();
                pack.setTime(dtf.format(LocalTime.now()));
                String str = new String(mesg);
                System.out.println("[SERVER] Дублирование никнейма "+str );
                byte[] msg = pack.createArr(4);
                for (ClientHandler ch : clientsList) {
                    if(ch.my_name.equals(str)){
                        ch.sendMsg(msg);
                        names.remove(str);
                    }
                }
            }
        }
    }

    public void run()  {
       // names.add("AlexeyNavalny");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner in = new Scanner(System.in);
                while (true) {
                    String mess = "";
                    if (in.hasNext() ) {
                       mess = in.nextLine();
                    }
                    if (mess.equals("stop server")) {
                        in.close();
                        disconnect();
                        break;
                    }
                }
            }
        }).start();
        try {
            serverSocket = new ServerSocket(port,0, InetAddress.getByName(host));
            System.out.println("[SERVER] Сервер запущен на порту:" +serverSocket.getLocalPort());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.out.println("Running Shutdown Hook");
                    byte[] b = new byte[3];
                    sendToAll(1,b);
                    work = false;
                    try {
                        serverSocket.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            });
            while (work){
                System.out.println("[SERVER] Ожидание подключений клиентов...");
                System.out.println("[SERVER] Для выключения сервера введите 'stop server'");
                clientSocket = serverSocket.accept();
                ClientHandler client  = new ClientHandler(clientSocket,this);
                System.out.println("Name: "+ client.my_name);
                if(names.contains(client.my_name)){
                    ClientMessege cm = new ClientMessege();
                    cm.setTime(dtf.format(LocalTime.now()));
                    byte[] msg = cm.createArr(4);
                    clientSocket.getOutputStream().write(msg);
                    clientSocket.getOutputStream().flush();
                    clientSocket.getOutputStream().close();
                    clientSocket.close();
                    removeClient(client);
                    names.remove(client.my_name);
                } else {
                    clientsList.add(client);
                    names.add(client.my_name);
                    new Thread(client).start();
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }
}
