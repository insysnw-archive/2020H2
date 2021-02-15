package com.alexandr.client;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.console().readLine());
        int port  = 0;
        String name = "";
        System.out.println("Введите порт сервера: ");
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
        //Client client = new Client(6666,"AlexeyNavaly");
        client.run();
    }

}
