package com.eziosoft.Dankcord;

import com.eziosoft.Dankcord.webAuth.webServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {

    private static Map<Integer, Client> clients = new HashMap<>();
    private static Random random = new Random();
    public static KeyPair serverkeys;

    public static void main(String[] args) throws IOException {
        System.out.println("Dankcord Server version 0.7 Alpha is starting up...");
        // generate server's encryption keys
        System.out.println("Generating server-side encryption keys...");
        try{
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            serverkeys = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e){
            System.out.println("There has been an error while trying to generate server-side encryption keys.");
            System.out.println("Dankcord will now exit. Please use the provided stack trace for help in debugging");
            e.printStackTrace();
            System.exit(-2);
        }
        System.out.println("Keys generated!");
        // init the database
        Database.databaseInit();

        // boot web server
        webServer web = new webServer(6970);
        try {
            web.startWeb();
        } catch (Exception e){
            e.printStackTrace();
        }

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
