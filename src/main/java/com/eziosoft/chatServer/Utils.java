package com.eziosoft.chatServer;

import com.google.gson.Gson;

public class Utils {

    private static Gson gson = new Gson();


    public static String getJson(Message m){
        return gson.toJson(m);
    }

    public static Message getMessage(String json){
        return gson.fromJson(json, Message.class);
    }
}
