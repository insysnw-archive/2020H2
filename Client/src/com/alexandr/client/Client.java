package com.alexandr.client;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Client implements Runnable {
    Integer port;
    String myName;
    Socket clientSocket;
    InputStream inMessage;
    OutputStream outMessage;
    Scanner scan;
    PrintWriter writer;
    int fl = 0;

    String SERVER_HOST = "localhost";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    Client(String host, Integer port, String name) {
        this.port = port;
        this.myName = name;
        this.SERVER_HOST = host;
    }

    private int connect() {
        try {
            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, port);
            inMessage = clientSocket.getInputStream();
            outMessage = clientSocket.getOutputStream();
            scan = new Scanner(inMessage);
            writer = new PrintWriter(outMessage);
            sendMsg(0,myName);
        } catch (IOException e) {
            System.out.println("Нет соединения с сервером. Зайдите позже:(");
            System.exit(-1);
            return -1;
        }
        return 0;
    }

    private void sendMsg(int flag, String mess) {
        switch (flag) {
            case 0 -> {
                ClientMessege cm = new ClientMessege();
                cm.setName(mess);
                LocalDateTime now = LocalDateTime.now();
                cm.setTime(dtf.format(now));
                cm.setText(" Вошел в чат");
                byte[] msg = cm.createArr(2);
                try {
                    clientSocket.getOutputStream().write(msg);
                    clientSocket.getOutputStream().flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            case 1 -> {
                ClientMessege cm = new ClientMessege();
                cm.setName(myName);
                LocalDateTime now = LocalDateTime.now();
                cm.setTime(dtf.format(now));
                cm.setText(mess);
                byte[] msg = cm.createArr(0);
                try {
                    clientSocket.getOutputStream().write(msg);
                    clientSocket.getOutputStream().flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            case 2 -> {
                ClientMessege cm = new ClientMessege();
                cm.setName(myName);
                LocalDateTime now = LocalDateTime.now();
                cm.setTime(dtf.format(now));
                cm.setText("Покинул чат");
                byte[] msg = cm.createArr(3);
                try {
                    clientSocket.getOutputStream().write(msg);
                    clientSocket.getOutputStream().flush();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void disconnect() {
        sendMsg(2, null);
        // clientSocket.close();
        //scan.close();
        // writer.close();
        // inMessage.close();
        // outMessage.close();
        System.out.print("Вы покинули чат");
        System.exit(0);
    }

    @Override
    public void run() {
        int conn = connect();
        if (conn == 0) {

            System.out.println("Для входа в чат нажмите Enter два раза");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (scan.hasNext()) {
                            byte[] data = new byte[2048];
                            ClientMessege cm = new ClientMessege();
                            cm.decodeMsg(data);
                            String res = "[" + cm.getTime() + "] " + cm.getName() + ": " + cm.getText();
                            if(cm.getText().length() == 0){
                                System.out.println("Дублирование ника");
                                break;
                            } else {
                                if(cm.getText().equals("Отключение сервера")){
                                    System.out.println("Потеря соединения с сервером");
                                    break;
                                } else {
                                    System.out.println(res);
                                }
                            }

                        }
                    }
                    System.exit(0);
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Scanner in = new Scanner(System.in);
                    while (true) {
                        String mess = "";
                        if (in.hasNext()) {
                            mess = in.nextLine();
                            if(fl<= 1){
                                sendMsg(1, mess);
                                fl++;
                            } else {
                                if(mess.length() !=0){
                                    sendMsg(1, mess);
                                }
                            }
                        }
                        if (mess.equals("close chat")) {
                            in.close();
                            disconnect();
                        }
                    }
                }
            }).start();

        } else {
            System.out.println("Че то пошло не так");
        }
    }
}
