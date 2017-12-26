/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.gsm.trackingsystem.testmachine;

/**
 *
 * @author abdhul
 */
public class User {
    private String name;
    private String surname;
    private String username;
    private boolean admin;
    private int loginID;
    
    public User(String name, String surname, String username, boolean admin, int loginID){
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.admin = admin;
        this.loginID = loginID;
    }
    
    public String getUsername(){
        return username;
    }
    
    public String getName(){
        return name;
    }
    
    public String getSurname(){
        return surname;
    }
    
    public boolean getAdmin(){
        return admin;
    }
    
    public int getLoginID(){
        return loginID;
    }
    
    public String getLoginIDToString(){
        Integer loginIDInteger = loginID;
        return loginIDInteger.toString();
    }
    
}
