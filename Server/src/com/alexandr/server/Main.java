package com.alexandr.server;


import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Введите порт: ");
        Scanner scan = new Scanner(System.console().readLine());
        int port  = 0;
        String host = "";
        if(scan.hasNext()){
            port = scan.nextInt();
        }
        scan.close();

        System.out.println("Введите хост сервера: ");
        Scanner sc1 = new Scanner(System.console().readLine());
        if (sc1.hasNext()){
            host = sc1.nextLine();
        }
        sc1.close();


        Server server = new Server(host,port);
        server.run();
    }
}
