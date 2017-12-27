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
public interface TestMachineListener {
    void dataPacketReceivedEvent(Board board);
    void displayError(String message);
    void errorStateEvent();
    void normalStateEvent();
    void boardNotGoodEvent();
    void loginOccurredEvent(User user);
    void connectedEvent();
    void disconnectedEvent();
    void logoutOccurredEvent();
}
