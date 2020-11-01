package com.eziosoft.Dankcord.webAuth;

import com.eziosoft.Dankcord.Database;
import com.eziosoft.Dankcord.Server;
import com.eziosoft.Dankcord.User;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class webServer {

    private int port;

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
            // generate an auth token via bcrypt and then encode it with base64
            String token = BCrypt.hashpw(dank.getUsername(), Server.authsalt);
            exchange.sendResponseHeaders(200, token.getBytes().length);
            exchange.getResponseBody().write(token.getBytes());
            exchange.getResponseBody().close();
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
