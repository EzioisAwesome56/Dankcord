package com.eziosoft.Dankcord;

public class authBlock {

    private String token;
    private String privatekeyb64;
    private String username;

    public authBlock(String token, String privatekeyb64, String username){
        this.token = token;
        this.privatekeyb64 = privatekeyb64;
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPrivatekeyb64() {
        return this.privatekeyb64;
    }

    public String getToken() {
        return this.token;
    }
}
