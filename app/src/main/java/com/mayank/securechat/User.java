package com.mayank.securechat;

public class User {
    String displayName;
    String uid;
    String emailId;
    String publicKey;


    public User(){

    }

    public User(String uid, String name, String email,String key) {
        this.uid = uid;
        this.displayName = name;
        this.emailId = email;
        this.publicKey=key;
    }

    /*
    public User(String uid, String name, String email,String key) {
        this.uid = uid;
        this.displayName = name;
        this.emailId = email;
        this.publicKey=key;
    }
    */


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUid() {
        return uid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
}
