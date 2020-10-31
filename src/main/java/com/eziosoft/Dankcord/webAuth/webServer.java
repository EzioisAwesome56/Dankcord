package com.eziosoft.Dankcord.webAuth;

import com.eziosoft.Dankcord.Server;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class webServer {

    private int port;

    public webServer(int a){
        this.port = a;
    }

    public void startWeb() throws Exception{
        System.out.println("Dankcord whweb interface is now starting...");
        // init the http server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // create "contexts" aka pages
        server.createContext("/auth", new authPage());
        server.createContext("/reg", new registerPage());
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


    static class authPage implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String text = queryToMap(exchange.getRequestURI().getQuery()).get("a");
            byte[] h = text.getBytes();
            exchange.sendResponseHeaders(200, h.length);
            OutputStream out = exchange.getResponseBody();
            out.write(h);
            out.close();
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
