package com.alexandr.server;


import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.console().readLine());
        int port  = 0;
        if(scan.hasNext()){
            port = scan.nextInt();
        }
        scan.close();
        Server server = new Server(port);
        //Server server = new Server(6666);
        server.run();
    }
}
