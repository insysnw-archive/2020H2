package com.alexandr.lyalin;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    static final int PORT = 6666;
		// список клиентов, которые будут подключаться к серверу
    private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
private boolean flag = true;
    public Server() {
				// сокет клиента, это некий поток, который будет подключаться к серверу
				// по адресу и порту
        Socket clientSocket = null;
				// серверный сокет
        ServerSocket serverSocket = null;
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Thread.sleep(200);
                        disconnecting();
                        flag = false;
                        System.out.println("Shouting down ...");
                        ProcessHandle.current().destroy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.exit(0);
                    } finally {
                        this.stop();
                        System.exit(0);
                    }
                }
            });
            // создаём серверный сокет на определенном порту
            serverSocket = new ServerSocket(PORT);
            new  Thread(new Runnable() {
                @Override
                public void run() {
                    String str = System.console().readLine();
                    if(str.equals("exit")){
                        flag = false;
                        disconnecting();
                        System.out.println("Shouting down ...");
                        System.exit(0);
                    }
                }
            }).start();
            if (flag) {
                // создаём серверный сокет на определенном порту
                System.out.println("Сервер запущен!");
                while (true) {
                    // таким образом ждём подключений от сервера
                    clientSocket = serverSocket.accept();
                    // создаём обработчик клиента, который подключился к серверу
                    // this - это наш сервер
                    ClientHandler client = new ClientHandler(clientSocket, this);
                    clients.add(client);
                    // каждое подключение клиента обрабатываем в новом потоке
                    new Thread(client).start();
                }
            } else {
                clientSocket.close();
                serverSocket.close();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
		

    public void sendMessageToAllClients(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void disconnecting(){
        sendMessageToAllClients("###end###");

    }


    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

}
