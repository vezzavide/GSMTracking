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
        if (code.length() != 24) return false;
        
        /*
         * EXPECTED FORMAT:
         *  [00][01][02][03][04][05][06][07][08]  [09]  [10][11][12][13][14][15]    [16]    [17][18]  [19]  [20][21][22][23]
         *  |___________________________________||____||________________________||_______||_________||____||________________|
         *                  0-8:                   9:            10-15:            16:       17-18:   19:        20-23:
         *          readable chars (not accents)  '-'           numbers         'A' or 'B'  numbers   'S'       numbers
         */
        
        for (int i = 0; i < 24; i++) {
            char charToCheck = code.charAt(i);
            if (i <= 8) {
                if (!(charToCheck >= 32) && (charToCheck <= 126)) {
                    return false;
                }
            } else if (i == 9) {
                if (charToCheck != '-') {
                    return false;
                }
            } else if ((i >= 10) && (i <= 15)) {
                if (!((charToCheck >= '0') && (charToCheck <= '9'))) {
                    return false;
                }
            } else if (i == 16) {
                if (!((charToCheck == 'A') || (charToCheck == 'B'))) {
                    return false;
                }
            } else if ((i == 17) || (i == 18)) {
                if (!((charToCheck >= '0') && (charToCheck <= '9'))) {
                    return false;
                }
            } else if (i == 19) {
                if (charToCheck != 'S') {
                    return false;
                }
            } else if (!((charToCheck >= '0') && (charToCheck <= '9'))) {
                return false;
            }
        }
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
