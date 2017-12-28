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
    void logoutScheduledEvent(String logout);
    // Server state:
    //  1: data sent and waiting for confirmation
    //  2: everything went alright
    //  3: error from server
    void serverStateEvent(int serverState);
}
