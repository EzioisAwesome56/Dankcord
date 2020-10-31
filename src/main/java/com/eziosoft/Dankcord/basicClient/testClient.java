package com.eziosoft.Dankcord.basicClient;

import com.eziosoft.Dankcord.Message;
import com.eziosoft.Dankcord.User;
import com.eziosoft.Dankcord.Utils;
import com.google.gson.Gson;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class testClient {

    public static DataInputStream in;
    public static DataOutputStream out;
    public static String name;
    public static Boolean isactive;
    private static String password;
    private static Gson gson = new Gson();
    private static byte[] privatekey;

    public static void main(String[] args){
        try {
            Scanner scn = new Scanner(System.in);
            Socket s = new Socket(InetAddress.getLocalHost(), 6969);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());

            // start up the message printer thread first and formost
            Thread msgdisplay = new Thread(){
                public void run(){
                    while (isactive) {
                        // check if theres anything on the input line
                        try {
                            if (in.available() > 0) {
                                // get the message from it
                                Message m = Utils.getMessage(in.readUTF());
                                // did the server just send back our own message?
                                if (!m.getUser().equals(name)) {
                                    System.out.println(m.getUser() + ": " + m.getContent());
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            // prompt for username
            System.out.println("What is your username?");
            name = scn.nextLine();
            System.out.println("Please enter your password");
            password = scn.nextLine();


            // communicate with the server :D
            String h = null;
            Boolean auth = false;
            while (true){
                if (!auth) {
                    h = in.readUTF();
                    if (h.equals("AUTH")) {
                        User epic = new User(name, password, "gamer");
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privatekey)));
                        out.writeUTF(Base64.getEncoder().encodeToString(cipher.doFinal(gson.toJson(epic).getBytes())));
                    } else if (h.equals("FAIL")) {
                        // dang
                        s.close();
                        System.out.println("Server rejected our auth data...");
                        break;
                    } else if (h.equals("OK")) {
                        // we have auth'd ok
                        System.out.println("We have auth'd with the server!");
                        auth = true;
                        isactive = true;
                        // start the message printer
                        msgdisplay.start();
                    } else if (h.equals("REG")){
                        // they do not have an account! we need to fix this now lol
                        System.out.println("Server has requested user account creation. Please enter your password twice to confirm it");
                        boolean match = false;
                        String pass1 = "no";
                        while (!match) {
                            pass1 = scn.nextLine();
                            String pass2 = scn.nextLine();
                            if (pass1.equals(pass2)){
                                System.out.println("Passwords match. Sending to server...");
                                match = true;
                            } else {
                                System.out.println("Passwords do not match! please try again");
                            }
                        }
                        User dank = new User(name, pass1, "gamer juice");
                        // encrypt data with public key from eariler
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privatekey)));
                        out.writeUTF(Base64.getEncoder().encodeToString(cipher.doFinal(gson.toJson(dank).getBytes())));
                    } else if (h.equals("KEY")){
                        // we are about to be sent our public key
                        // tell the server we are ready for it
                        out.writeUTF("OK");
                    } else {
                        // this should be the public key
                        privatekey = Base64.getDecoder().decode(h);
                    }
                }
                // deal with user input here
                if (auth) {
                    h = scn.nextLine();
                    if (h.contains("DC")) {
                        out.writeUTF("DC");
                        s.close();
                        isactive = false;
                        break;
                    } else {
                        out.writeUTF(Utils.getJson(new Message(name, h)));
                    }
                }

            }
            out.close();
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
