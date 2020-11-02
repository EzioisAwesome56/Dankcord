package com.eziosoft.Dankcord;

public class authBlock {

    private String token;
    private String keyb64;
    private String username;

    public authBlock(String token, String keyb64, String username){
        this.token = token;
        this.keyb64 = keyb64;
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public String getkeyb64() {
        return this.keyb64;
    }

    public String getToken() {
        return this.token;
    }
}
