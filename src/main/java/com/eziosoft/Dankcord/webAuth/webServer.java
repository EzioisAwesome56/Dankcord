package com.eziosoft.Dankcord.webAuth;

import com.eziosoft.Dankcord.Database;
import com.eziosoft.Dankcord.Server;
import com.eziosoft.Dankcord.User;
import com.eziosoft.Dankcord.authBlock;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class webServer {

    private int port;
    private static SecureRandom sr = new SecureRandom();
    private static Gson gson = new Gson();

    public webServer(int a){
        this.port = a;
    }

    public void startWeb() throws Exception{
        System.out.println("Dankcord web interface is now starting...");
        // init the http server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // create "contexts" aka pages
        server.createContext("/auth", new authPage());
        server.createContext("/reg", new registerPage());
        server.createContext("/submit", new registerProcessor());
        server.setExecutor(null);
        server.start();
        System.out.println("Web server has finished loading!");
    }

    // from https://stackoverflow.com/questions/11640025/how-to-obtain-the-query-string-in-a-get-with-java-httpserver-httpexchange
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

    static class registerProcessor implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // get post data from the request body
            Map<String, String> data = queryToMap(IOUtils.toString(exchange.getRequestBody(), Charset.defaultCharset()));
            // check if the username is taken
            if (Database.checkForUser(data.get("username"))){
                String error = "Username already taken!";
                exchange.sendResponseHeaders(200, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
                return;
            }
            // check if the 2 entered passwords match
            if (!data.get("passone").equals(data.get("passtwo"))){
                String error = "provided passwords do not match! please try again";
                exchange.sendResponseHeaders(200, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
                return;
            }
            // then we go through the process of making the new user account
            // first off, generate the salt
            String salt = BCrypt.gensalt();
            // then generate the password hash
            String hash = BCrypt.hashpw(data.get("passone"), salt);
            // create a new user object
            User dank = new User(data.get("username"), hash, salt);
            // finally, save this new object into rethinkDB
            Database.saveUser(dank);
            // make the browser happy
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, "Your account has been created!".getBytes().length);
            os.write("Your account has been created!".getBytes());
            os.close();
        }
    }


    static class authPage implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // get the post data
            Map<String, String> data = queryToMap(IOUtils.toString(exchange.getRequestBody(), Charset.defaultCharset()));
            // check if the user exists
            if (!Database.checkForUser(data.get("username"))){
                // username does not exist
                // inform client of this and disconnect them
                String error = "account does not exist!";
                exchange.sendResponseHeaders(200, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
                return;
            }
            // load user object
            User dank = Database.loadUser(data.get("username"));
            // hash the provided password
            String hash = BCrypt.hashpw(data.get("password"), dank.getSalt());
            // check if the passwords match
            if (!dank.getPasshash().equals(hash)){
                // passwords do not match
                // gtfo
                String error = "password invalid!";
                exchange.sendResponseHeaders(200, error.getBytes().length);
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
                return;
            }
            // generate an auth token via bcrypt
            String token = BCrypt.hashpw(dank.getUsername(), BCrypt.gensalt());
            // generate asymetric keypair
            KeyPair pair = null;
            try{
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048, sr);
                pair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException e){
                System.out.println("How did this happen?");
                exchange.sendResponseHeaders(200, "YOU SHOULD NOT BE SEEING THIS!".getBytes().length);
                exchange.getResponseBody().write("YOU SHOULD NOT BE SEEING THIS!".getBytes().length);
                exchange.getResponseBody().close();
            }
            // dump all this crap into a new authBlock
            authBlock block = new authBlock(token, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()), Base64.getEncoder().encodeToString(Server.serverkeys.getPublic().getEncoded()));
            // encode authBlock to json
            String json = gson.toJson(block);
            // encode THAT into base64
            String b64json = Base64.getEncoder().encodeToString(json.getBytes());
            // send that to the client
            exchange.sendResponseHeaders(200, b64json.getBytes().length);
            exchange.getResponseBody().write(b64json.getBytes());
            exchange.getResponseBody().close();
            // now that we dont have to worry about the client, we have to worry about the server!
            // reuse old variables to make a new authBlock object
            block = new authBlock(token, Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()), dank.getUsername());
            // save this to database for use by actual chat client
            Database.saveAuthBlock(block);
            // and thats all she wrote!
        }
    }

    static class registerPage implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // first read the reg.html file as a byte[]
            byte[] h = IOUtils.toByteArray(registerPage.class.getResource("/reg.html"));
            // handle the client next
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, h.length);
            OutputStream out = exchange.getResponseBody();
            out.write(h);
            out.close();
        }
    }
}
