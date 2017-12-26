/*
 * TestMachine GUI made automatically with Netbeans' window builder (with swing)
 */
package it.gsm.trackingsystem.testmachine;

import com.fazecast.jSerialComm.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import javax.swing.text.BadLocationException;
import javax.swing.Timer;
import java.io.File;
/**
 *
 * @author abdhul
 */
public class TestMachineForm extends javax.swing.JFrame implements TestMachineListener{
    private TestMachine testMachine;
    private FileHandler fileHandler;
    private Logger logger;
    private String workingDirectory = System.getProperty(("user.dir"));
    private Timer errorStateBlinkTimer;
    private Timer boardNotGoodStateBlinkTimer;
    
    /**
     * Creates new form TestMachine
     */
    public TestMachineForm(){
        System.out.println(System.getProperty("user.dir"));
        logger = Logger.getLogger(TestMachineForm.class.getName());
        // Creates application
        testMachine = new TestMachine();
        // Add this instance of TestMachineForm to TestMachine listeners
        // so that TestMachine can send events to TestMachineForm
        testMachine.addListener(this);
        initComponents();
        populateComboBox();
        
        try {
            fileHandler = new FileHandler(workingDirectory + File.separator + "test_machine_GUI_errors.log", true);
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(TestMachineForm.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Creates and shows login form
        new LoginFrame(this, testMachine).setVisible(true);
    }
    
    @Override
    public void dataPacketReceivedEvent(Board board){
        boardTextField.setText(board.getCode());
        if (board.isGood()){
            testOutcomeTextField.setText("Good");
            testOutcomeTextField.setBackground(Color.green);
        } else{
            testOutcomeTextField.setText("Not good");
            testOutcomeTextField.setBackground(Color.ORANGE);
        }
        
    }
    
    @Override
    public void displayError(String message){
        errorTextArea.setText(message);
        errorDialog.pack();
        errorDialog.setVisible(true);
    }
    
    @Override
    public void errorStateEvent(){
        goodToGoButton.setEnabled(true);
        reScanButton.setEnabled(true);
        
        boardTextField.setText("Errore da Arduino");
        testOutcomeTextField.setText("");
        testOutcomeTextField.setBackground(null);
        
        int delay = 500; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            boolean toggle = true;
            public void actionPerformed(ActionEvent evt) {
                //...Perform a task...
                if (toggle){
                    toggle = false;
                    goodToGoButton.setBackground(Color.ORANGE);
                    reScanButton.setBackground(Color.ORANGE);
                }
                else{
                    toggle = true;
                    goodToGoButton.setBackground(null);
                    reScanButton.setBackground(null);
                }
                
            }
        };
        errorStateBlinkTimer = new Timer(delay, taskPerformer);
        errorStateBlinkTimer.start();
    }
    
    @Override
    public void normalStateEvent(){
        //boardTextField.setText("");
        //testOutcomeTextField.setText("");
        //testOutcomeTextField.setBackground(null);
        goodToGoButton.setEnabled(false);
        reScanButton.setEnabled(false);
        goodToGoButton.setBackground(null);
        reScanButton.setBackground(null);
        
        try{
            errorStateBlinkTimer.stop();
        }
        catch (NullPointerException ex){}
        
        try{
            boardNotGoodStateBlinkTimer.stop();
        }
        catch (NullPointerException ex){}
    }
    
    @Override
    public void boardNotGoodEvent(){
        goodToGoButton.setEnabled(true);
        
        int delay = 500; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            boolean toggle = true;
            public void actionPerformed(ActionEvent evt) {
                //...Perform a task...
                if (toggle){
                    toggle = false;
                    goodToGoButton.setBackground(Color.ORANGE);
                }
                else{
                    toggle = true;
                    goodToGoButton.setBackground(null);
                }
                
            }
        };
        boardNotGoodStateBlinkTimer = new Timer(delay, taskPerformer);
        boardNotGoodStateBlinkTimer.start();
    }
    
    @Override
    public void loginOccurredEvent(User user){
        currentUserLabel.setText("Operatore corrente: " + user.getSurname() + " " + user.getName() + " ");
    }
    
    private void populateComboBox(){
        //Populate combobox with available serial ports
        SerialPort[] ports = testMachine.getAvailableSerialPorts();
        for(int i = 0; i < ports.length; i++){
            serialPortComboBox.addItem(ports[i].getSystemPortName());
        }
    }
    
    private SerialPort getSelectedSerialPort(){
        for(SerialPort port : SerialPort.getCommPorts()){
            if (port.getSystemPortName().equals(serialPortComboBox.getSelectedItem().toString())){
                return port;
            }
        }
        return null;
    }
    
    // Shortens the terminal so that it won't ever be bigger
    // than 500 characters.
    /*
    private void trimTerminal(){
        if (terminalTextArea.getDocument().getLength() > 2000) {
            try {
                terminalTextArea.getDocument().remove(0, 30);
            } catch (BadLocationException ex) {
                Logger.getLogger(TestMachineForm.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    */

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        errorDialog = new javax.swing.JDialog();
        okButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        errorTextArea = new javax.swing.JTextArea();
        serialPortComboBox = new javax.swing.JComboBox<>();
        connectButton = new javax.swing.JButton();
        reScanButton = new javax.swing.JButton();
        goodToGoButton = new javax.swing.JButton();
        boardTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        testOutcomeTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        currentUserLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        logoutMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();

        errorDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        errorDialog.setTitle("Error!");
        errorDialog.setSize(new java.awt.Dimension(30, 30));

        okButton.setText("Ok");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        errorTextArea.setEditable(false);
        errorTextArea.setColumns(20);
        errorTextArea.setRows(5);
        jScrollPane1.setViewportView(errorTextArea);

        javax.swing.GroupLayout errorDialogLayout = new javax.swing.GroupLayout(errorDialog.getContentPane());
        errorDialog.getContentPane().setLayout(errorDialogLayout);
        errorDialogLayout.setHorizontalGroup(
            errorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, errorDialogLayout.createSequentialGroup()
                .addGroup(errorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(errorDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(errorDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        errorDialogLayout.setVerticalGroup(
            errorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(errorDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Tracking System - Macchina");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        connectButton.setBackground(new java.awt.Color(104, 255, 0));
        connectButton.setText("Start");
        connectButton.setToolTipText("");
        connectButton.setBorderPainted(false);
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        reScanButton.setText("Re-scan");
        reScanButton.setEnabled(false);
        reScanButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reScanButtonActionPerformed(evt);
            }
        });

        goodToGoButton.setText("Procedi");
        goodToGoButton.setEnabled(false);
        goodToGoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goodToGoButtonActionPerformed(evt);
            }
        });

        boardTextField.setEditable(false);
        boardTextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);

        jLabel2.setText("Ultima scheda:");

        jLabel3.setText("Esito test:");

        currentUserLabel.setFont(new java.awt.Font("Ubuntu", 0, 18)); // NOI18N
        currentUserLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        currentUserLabel.setText("Operatore corrente:");
        currentUserLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jMenu1.setText("Operatore");

        logoutMenuItem.setText("Logout");
        logoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(logoutMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Impostazioni");

        settingsMenuItem.setText("Impostazioni");
        settingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(settingsMenuItem);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(currentUserLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(serialPortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(connectButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reScanButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(goodToGoButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(boardTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(0, 78, Short.MAX_VALUE))
                            .addComponent(testOutcomeTextField))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serialPortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(connectButton)
                    .addComponent(reScanButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(goodToGoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boardTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(testOutcomeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addComponent(currentUserLabel)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        testMachine.closePort();
        fileHandler.close();
        
    }//GEN-LAST:event_formWindowClosing

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        // Connect to port
        if(connectButton.getText().equals("Start")){
            if(testMachine.connect(getSelectedSerialPort())){
                connectButton.setText("Stop");
                connectButton.setBackground(Color.RED);
                serialPortComboBox.setEnabled(false);
            }
        }
        // Disconnect to port
        else{
            testMachine.disconnect();
            connectButton.setText("Start");
            connectButton.setBackground(Color.GREEN);
            serialPortComboBox.setEnabled(true);
            goodToGoButton.setEnabled(false);
            reScanButton.setEnabled(false);
        }
        
    }//GEN-LAST:event_connectButtonActionPerformed

    private void goodToGoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goodToGoButtonActionPerformed
        // TODO add your handling code here:
        testMachine.sendGoodToGoSignal();
        normalStateEvent();
    }//GEN-LAST:event_goodToGoButtonActionPerformed

    private void reScanButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reScanButtonActionPerformed
        // TODO add your handling code here:
        testMachine.sendReScanSignal();
        normalStateEvent();
    }//GEN-LAST:event_reScanButtonActionPerformed
   
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        // TODO add your handling code here:
        errorDialog.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void logoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutMenuItemActionPerformed
        // TODO add your handling code here:
        testMachine.disconnect();
        connectButton.setText("Start");
        connectButton.setBackground(Color.GREEN);
        serialPortComboBox.setEnabled(true);
        goodToGoButton.setEnabled(false);
        reScanButton.setEnabled(false);
        this.setVisible(false);
        new LoginFrame(this, testMachine).setVisible(true);
    }//GEN-LAST:event_logoutMenuItemActionPerformed

    private void settingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsMenuItemActionPerformed
        // TODO add your handling code here:
        new settingsFrame(this, testMachine).setVisible(true);
    }//GEN-LAST:event_settingsMenuItemActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TestMachineForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TestMachineForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TestMachineForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TestMachineForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new TestMachineForm().setVisible(true);
                new TestMachineForm();
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField boardTextField;
    private javax.swing.JButton connectButton;
    private javax.swing.JLabel currentUserLabel;
    private javax.swing.JDialog errorDialog;
    private javax.swing.JTextArea errorTextArea;
    private javax.swing.JButton goodToGoButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenuItem logoutMenuItem;
    private javax.swing.JButton okButton;
    private javax.swing.JButton reScanButton;
    private javax.swing.JComboBox<String> serialPortComboBox;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JTextField testOutcomeTextField;
    // End of variables declaration//GEN-END:variables
}
