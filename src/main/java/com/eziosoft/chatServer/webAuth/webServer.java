package com.eziosoft.chatServer.webAuth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class webServer {

    private int port;

    public webServer(int a){
        this.port = a;
    }

    public void startWeb() throws Exception{
        System.out.println("Dankcord web interface is now starting...");
        // init the http server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/test", new test());
        server.setExecutor(null);
        server.start();
        System.out.println("Web server has finished loading!");
    }


    static class test implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] h = "gamer fuel".getBytes();
            exchange.sendResponseHeaders(200, h.length);
            OutputStream out = exchange.getResponseBody();
            out.write(h);
            out.close();
        }
    }
}
