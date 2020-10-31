package com.eziosoft.Dankcord;

public class User {

    //private String uid;
    private String passhash;
    private String username;
    private String salt;

    public User(String username, String hash, String salt){
        this.passhash = hash;
        this.username = username;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getSalt() {
        return salt;
    }

    public String getPasshash() {
        return passhash;
    }
}
