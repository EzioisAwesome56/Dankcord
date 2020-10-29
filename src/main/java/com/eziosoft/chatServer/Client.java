package com.eziosoft.chatServer;


import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Client {

    final Socket s;
    final DataInputStream in;
    final DataOutputStream out;
    private static String get;
    private static String shit;
    private static Gson gson = new Gson();
    private static byte[] publickey;
    private SecureRandom sr = new SecureRandom();
    private int conid;

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
                    // first we need to establish encryption to the client
                    out.writeUTF("KEY");
                    if (in.readUTF().equals("OK")){
                        // prepare and send the key
                        KeyPair pair;
                        try {
                            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                            kpg.initialize(2048, sr);
                            pair = kpg.generateKeyPair();
                        } catch (NoSuchAlgorithmException e){
                            e.printStackTrace();
                            System.out.println("Critical error while trying to enable encryption!");
                            s.close();
                            out.close();
                            in.close();
                            Server.deleteClient(conid);
                            return;
                        }
                        // store private key for use by server
                        publickey = pair.getPublic().getEncoded();
                        // send the client the public key
                        out.writeUTF(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
                    } else {
                        System.out.println("Client is not acting correctly! GTFO");
                        s.close();
                        out.close();
                        in.close();
                        Server.deleteClient(conid);
                        return;
                    }
                    // try to auth client
                    out.writeUTF("AUTH");
                    get = in.readUTF();
                    // decrypt this shit
                    try {
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publickey)));
                        shit = new String(cipher.doFinal(Base64.getDecoder().decode(get)));
                    } catch (Exception e){
                        e.printStackTrace();
                        System.exit(-2);
                    }

                    // load a user object from the obtained data
                    User main = gson.fromJson(shit, User.class);
                    // check if supplied username is in database
                    if (!Database.checkForUser(main.getUsername())){
                        // handle user registration
                        out.writeUTF("REG");
                        // wait for new data to be written
                        while (true){
                            if (in.available() > 0){
                                get = in.readUTF();
                                break;
                            }
                        }
                        // make a new user object
                        User dank = gson.fromJson(get, User.class);
                        // this user object is awful tho, we need to hash the password first
                        // to do this, first we need a SALT
                        String salt = BCrypt.gensalt();
                        // then we can hash the password
                        String hash = BCrypt.hashpw(dank.getPasshash(), salt);
                        // then load all this into a new user object
                        dank = new User(dank.getUsername(), hash, salt);
                        // dump this shit into the database
                        Database.saveUser(dank);

                        // finally done! send ok
                        out.writeUTF("OK");
                    } else {
                        // get password from object
                        String pass = main.getPasshash();
                        // load user account from database
                        main = Database.loadUser(main.getUsername());
                        // check if the hashes match
                        if (!BCrypt.hashpw(pass, main.getSalt()).equals(main.getPasshash())){
                            // they dont match! GTFO
                            out.writeUTF("FAIL");
                            s.close();
                            out.close();
                            in.close();
                            Server.deleteClient(conid);
                            return;
                        } else {
                            // send the ok
                            out.writeUTF("OK");
                        }
                    }
                    // main loop
                    while (true){
                        get = in.readUTF();
                        if (get.equals("DC")){
                            s.close();
                            System.out.println("Client has disconnected!");
                            break;
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
            out.writeUTF(content);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
