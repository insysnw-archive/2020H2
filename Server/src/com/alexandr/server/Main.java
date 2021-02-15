package com.alexandr.server;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Введите порт: ");
        Scanner scan = new Scanner(System.console().readLine());
        int port  = 0;
        if(scan.hasNext()){
            port = scan.nextInt();
        }
        scan.close();
        Server server = new Server(port);
        server.run();
    }
}
