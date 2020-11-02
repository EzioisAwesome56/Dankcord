package com.eziosoft.Dankcord;

import com.google.gson.Gson;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Client {

    final Socket s;
    final DataInputStream in;
    final DataOutputStream out;
    private static String get;
    private static String shit;
    private static Gson gson = new Gson();
    private static PublicKey publickey;
    private SecureRandom sr = new SecureRandom();
    private int conid;
    private String name;
    private Cipher encrypt;
    private Cipher decrypt;


    public Client(Socket s, DataInputStream i, DataOutputStream o, int conid){
        this.s = s;
        this.in = i;
        this.out = o;
        this.conid = conid;
        // gamers
    }

    public int getConid() {
        return conid;
    }

    public void connect(){
        // start the client's thread or something idk
        Thread loop = new Thread(){
            public void run(){
                try {
                    // setup server side encrption first
                    try {
                        encrypt = Cipher.getInstance("RSA");
                        encrypt.init(Cipher.ENCRYPT_MODE, Server.serverkeys.getPrivate());
                        // quickly init the decryption cipher
                        decrypt = Cipher.getInstance("RSA");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    // try sending the message to the client
                    try{
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("USER".getBytes())));
                    } catch (Exception e){
                        System.out.println("Error while trying to encrypt message to client!");
                        e.printStackTrace();
                    }
                    name = in.readUTF();
                    // check if the account exists
                    if (!Database.checkForUser(name)){
                        // account does not exist, disconnect client
                        try {
                            out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("FAIL".getBytes())));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        s.close();
                        out.close();
                        in.close();
                        Server.deleteClient(conid);
                        return;
                    }
                    // load authBlock object for user
                    authBlock block = Database.loadAuthBlock(name);
                    // quickly load public key
                    try{
                        publickey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(block.getkeyb64())));
                        decrypt.init(Cipher.DECRYPT_MODE, publickey);
                    } catch (Exception e){
                        // critical error, this should not happen
                        e.printStackTrace();
                        System.out.println("HOW DID THIS HAPPEN?");
                        System.exit(-2);
                    }
                    // ask the client for its authentication token
                    try{
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("AUTH".getBytes())));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    get = in.readUTF();
                    // decrypt this shit
                    try {
                        shit = new String(decrypt.doFinal(Base64.getDecoder().decode(get)));
                    } catch (Exception e){
                        e.printStackTrace();
                        System.out.println("HOW DID THIS HAPPEN?");
                        System.exit(-2);
                    }
                    // compare the auth token with the one in the database
                    if (!shit.equals(block.getToken())){
                        // token invalid! disconnect client
                        out.writeUTF("FAIL");
                        s.close();
                        out.close();
                        in.close();
                        Server.deleteClient(conid);
                        return;
                    }
                    // if we are here, it means the client checks out as legitimate
                    try{
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("OK".getBytes())));
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    // main loop
                    while (true){
                        get = in.readUTF();
                        try{
                            shit = new String(decrypt.doFinal(Base64.getDecoder().decode(get)));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        if (shit.equals("&DC")){
                            s.close();
                            System.out.println("Client has disconnected!");
                            break;
                        } else if (shit.equals("&DELAUTH")){
                            // Delete stored auth block for user
                            Database.deleteAuthBlock(name);
                        } else {
                            // deal with messages
                            System.out.println("Message: "+ get);
                            Server.sendAll(get);
                        }
                    }
                    in.close();
                    out.close();
                    Server.deleteClient(conid);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
        // start the loop
        loop.setDaemon(true);
        loop.start();
    }

    public void send(String content) {
        try {
            out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal(content.getBytes())));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
