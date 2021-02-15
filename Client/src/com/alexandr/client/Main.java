package com.alexandr.client;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Введите порт сервера: ");
        Scanner scan = new Scanner(System.console().readLine());
        int port  = 0;
        String name = "";
        if(scan.hasNext()){
            port = scan.nextInt();
        }
        scan.close();
        System.out.println("Введите имя: ");
        Scanner sc = new Scanner(System.console().readLine());
        if (sc.hasNext()){
            name = sc.nextLine();
        }
        sc.close();
        Client client = new Client(port,name);
        client.run();
    }

}
