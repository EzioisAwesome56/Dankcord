package com.eziosoft.chatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {

    private static Map<Integer, Client> clients = new HashMap<>();
    private static Random random = new Random();

    public static void main(String[] args) throws IOException {
        System.out.println("Dankcord Server version 0.1 Alpha is starting up...");
        // init the database
        Database.databaseInit();

        // init the server
        ServerSocket ss = new ServerSocket(6969);

        // run loop to get clients
        System.out.println("Dankcord Server now accepting client connections...");
        while (true){
            Socket s = null;
            try {
                s = ss.accept();
                System.out.println("Client Connected! "+ s);
                // get data in and out streams
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                DataInputStream in = new DataInputStream(s.getInputStream());
                // make a new client object
                Client c = new Client(s, in, out, random.nextInt());
                // add it to the list
                clients.put(c.getConid(), c);
                // start the client
                c.connect();


            } catch (Exception e){
                s.close();
                e.printStackTrace();
            }
        }
    }

    public static void sendAll(String content){
        for(Map.Entry<Integer, Client> c : clients.entrySet()){
            c.getValue().send(content);
        }
    }

    public static void deleteClient(Integer id){
        if (clients.containsKey(id)){
            clients.remove(id);
        }
    }
}
