/*
 * This class represents a board travelling through the tracking system
 */
package it.gsm.trackingsystem.testmachine;

/**
 *
 * @author abdhul
 */
public class Board {
    private String code = "";
    private Boolean good = null;
    
    public Board() {
        
    }
    
    public Board(String code){
        this.code = code;
    }
    
    public Board(String code, Boolean good){
        this.code = code;
        this.good = good;
    }
    
    public void setCode(String code){
        this.code =  code;
    }
    
    public void setGood(Boolean good){
        this.good = good;
    }
    
    public String getCode(){
        return this.code;
    }
    
    public boolean isGood(){
        return this.good;
    }
    
    public String isGoodString(){
        if(this.good){
            return "1";
        }
        else{
            return "0";
        }
    }
    
    public boolean isFilled(){
        if (this.code.length() == 24){
            return true;
        }else{
            return false;
        }
    }
    
    public boolean check(){
        // TODO: implement check on code format
        return true;
    }
    
    public void append(String piece){
        this.code += piece;
    }
    
    public void append(char character){
        String charString = new String(Character.toString(character));
        this.code += charString;
    }
    
}
