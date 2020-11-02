package com.eziosoft.Dankcord;

import com.google.gson.Gson;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;

public class Database {

    private static Connection thonk;
    private static final RethinkDB r = RethinkDB.r;
    private static Gson gson = new Gson();

    public static void databaseInit(){
        System.out.println("Eziosoft Database driver starting up...");
        // build the connection
        Connection.Builder builder = r.connection().hostname("localhost").port(28015);
        builder.user("admin", "");
        thonk = builder.connect();

        // check if the database already exists
        System.out.println("Checking if database exists...");
        if (!r.dbList().contains("Dankcord").run(thonk, Boolean.class).first()){
            System.out.println("Database does not exist! creating it...");
            r.dbCreate("Dankcord").run(thonk);
            r.db("Dankcord").tableCreate("users").optArg("primary_key", "username").run(thonk);
            r.db("Dankcord").tableCreate("channels").optArg("primary_key", "name").run(thonk);
            r.db("Dankcord").tableCreate("auth").optArg("primary_key", "token").run(thonk);
            System.out.println("Database created!");
        } else {
            System.out.println("Database already exists!");
        }
        // bind our connection to it
        thonk.use("Dankcord");
        // init done
        System.out.println("Database driver has finished starting up!");
    }

    public static boolean checkForUser(String email){
        return r.table("users").getAll(email).count().eq(1).run(thonk, boolean.class).first();
    }

    public static void saveUser(User user){
        r.table("users").insert(user).run(thonk);
    }

    public static User loadUser(String name){
        return gson.fromJson(r.table("users").get(name).toJson().run(thonk, String.class).first(), User.class);
    }

    public static void saveAuthBlock(authBlock auth){
        r.table("auth").insert(auth).run(thonk);
    }

}
