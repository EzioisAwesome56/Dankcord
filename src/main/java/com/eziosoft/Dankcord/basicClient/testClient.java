package com.eziosoft.Dankcord.basicClient;

import com.eziosoft.Dankcord.Message;
import com.eziosoft.Dankcord.authBlock;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class testClient {

    public static DataInputStream in;
    public static DataOutputStream out;
    public static String name;
    public static Boolean isactive;
    private static String password;
    private static Gson gson = new Gson();
    private static PrivateKey privatekey;
    private static PublicKey serverkey;
    private static Cipher encrypt;
    private static Cipher decrypt;
    private static String shit;

    public static void main(String[] args){
        try {
            Scanner scn = new Scanner(System.in);

            // start up the message printer thread first and formost
            Thread msgdisplay = new Thread(){
                public void run(){
                    while (isactive) {
                        // check if theres anything on the input line
                        try {
                            if (in.available() > 0) {
                                shit = in.readUTF();
                                // get message objects
                                // TODO: figure out why i cant encrypt messages server -> client
                                Message m = gson.fromJson(shit, Message.class);
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

            // try talking to the https endpoint
            URL url = new URL("https://test.fuck/auth");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(999999999);
            conn.setReadTimeout(999999999);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            Map<String, String> params = new HashMap<>();
            params.put("username", name);
            params.put("password", password);
            // properly format the post data
            StringJoiner sj = new StringJoiner("&");
            for(Map.Entry<String,String> entry : params.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
            byte[] what = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = what.length;
            conn.setFixedLengthStreamingMode(length);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.connect();
            try(OutputStream os = conn.getOutputStream()) {
                os.write(what);
            }
            // check if the string is an error
            String json = IOUtils.toString(conn.getInputStream(), Charset.defaultCharset());
            if (json.equals("account does not exist!")){
                System.out.println("the account you requested to login as does not exist.");
                // TODO: make client print the register link
                System.out.println("please register one at UNFINISHED FEATURE");
                System.exit(-1);
            } else if (json.equals("password invalid!")){
                System.out.println("Your password is incorrect. Please try again");
                System.exit(-1);
            }
            // if we survived the if block, then we must have gotten the data we actually want
            json = new String(Base64.getDecoder().decode(json), StandardCharsets.UTF_8);
            // convert this to an authBlock object because yes.
            authBlock block = gson.fromJson(json, authBlock.class);
            // load in the keys
            privatekey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(block.getkeyb64())));
            serverkey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(block.getUsername())));
            // setup the 2 required ciphers
            try{
                encrypt = Cipher.getInstance("RSA");
                decrypt = Cipher.getInstance("RSA");
                encrypt.init(Cipher.ENCRYPT_MODE, privatekey);
                decrypt.init(Cipher.DECRYPT_MODE, serverkey);
            } catch (Exception e){
                System.out.println("Error while trying to configure ciphers!");
                e.printStackTrace();
                System.exit(-2);
            }

            // now we can actually start talking with the server
            Socket s = new Socket(InetAddress.getLocalHost(), 6969);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            // communicate with the server :D
            String h = null;
            Boolean auth = false;
            while (true){
                if (!auth) {
                    h = in.readUTF();
                    try{
                        h = new String(decrypt.doFinal(Base64.getDecoder().decode(h)));
                        System.out.println(h);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    if (h.equals("AUTH")) {
                        // server wants our auth token
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal(block.getToken().getBytes())));
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
                    } else if (h.equals("USER")){
                        // send the user name of the user we are trying to login as
                        // the server doesnt actually have our decryption key loaded yet, so send this plain-text
                        out.writeUTF(name);
                    } else {
                        System.out.println("Server sent weird data? wtf");
                    }
                }
                // deal with user input here
                if (auth) {
                    h = scn.nextLine();
                    if (h.contains("ploxdc")) {
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("&DC".getBytes())));
                        s.close();
                        isactive = false;
                        break;
                    } else if (h.contains("delete auth")) {
                        System.out.println("Asking server to delete stored auth token...");
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal("&DELAUTH".getBytes())));
                    } else {
                        out.writeUTF(Base64.getEncoder().encodeToString(encrypt.doFinal(gson.toJson(new Message(name, h)).getBytes())));
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
