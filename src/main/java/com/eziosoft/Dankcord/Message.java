package com.eziosoft.Dankcord;

public class Message {

    private String usr;
    private String content;

    public Message(String usr, String content){
        this.usr = usr;
        this.content = content;
    }

    public String getUser(){
        return this.usr;
    }

    public String getContent(){
        return this.content;
    }
}
