/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.gsm.trackingsystem.testmachine;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import java.util.*;
import java.time.*;
import javax.swing.ImageIcon;

/**
 *
 * @author abdhul
 */
public class settingsFrame extends javax.swing.JFrame {
    private javax.swing.JFrame parentForm;
    private TestMachine testMachine;

    /**
     * Creates new form settingsFrame
     */
    public settingsFrame(javax.swing.JFrame parentForm, TestMachine testMachine) {
        // Sets icon
        ImageIcon icon = new ImageIcon(getClass().getResource("/it/gsm/trackingsystem/testmachine/favicon.png"));
        setIconImage(icon.getImage());
        initComponents();
        this.parentForm = parentForm;
        this.testMachine = testMachine;
        populateSerialPortComboBox();
        
        machineTextField.setText(testMachine.getMachine());
        databaseServerNameTextField.setText(testMachine.getDatabaseServerName());
        databaseUserTextField.setText(testMachine.getDatabaseUser());
        databasePasswordTextField.setText(testMachine.getDatabasePassword());
        autostartCheckBox.setSelected(testMachine.isAutostartEnabled());
        debugCheckBox.setSelected(testMachine.isDebugEnabled());
        // necessary since with javax.swing.ListModel items can' be added
        scheduledLogoutList.setModel(new DefaultListModel<>());
        if(testMachine.getScheduledLogouts() != null){
            for (LocalTime logout : testMachine.getScheduledLogouts()){
                ((DefaultListModel) scheduledLogoutList.getModel()).addElement(logout);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        databaseServerNameTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        databaseUserTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        databasePasswordTextField = new javax.swing.JTextField();
        autostartCheckBox = new javax.swing.JCheckBox();
        debugCheckBox = new javax.swing.JCheckBox();
        serialPortComboBox = new javax.swing.JComboBox<>();
        machineTextField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        addScheduledLogoutButton = new javax.swing.JButton();
        deleteScheduledLogoutButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        scheduledLogoutList = new javax.swing.JList<>();
        applyButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Tracking System - Macchina - Impostazioni");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jLabel1.setText("Porta seriale di default:");

        jLabel2.setText("Macchina:");

        jLabel3.setText("Indirizzo server MySQL:");

        databaseServerNameTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel4.setText("Utente MySQL:");

        databaseUserTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel5.setText("Password utente MySQL:");

        databasePasswordTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        autostartCheckBox.setText("Autostart dopo il login (se possibile)");

        debugCheckBox.setText("Debug");

        serialPortComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                serialPortComboBoxPopupMenuWillBecomeVisible(evt);
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        machineTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(databasePasswordTextField)
                            .addComponent(databaseUserTextField)
                            .addComponent(databaseServerNameTextField)
                            .addComponent(serialPortComboBox, 0, 172, Short.MAX_VALUE)
                            .addComponent(machineTextField, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(autostartCheckBox)
                            .addComponent(debugCheckBox))
                        .addGap(0, 147, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(serialPortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(machineTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(databaseServerNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(databaseUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(databasePasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(autostartCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(debugCheckBox)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jTabbedPane4.addTab("Generali", jPanel1);

        addScheduledLogoutButton.setText("Aggiungi");
        addScheduledLogoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addScheduledLogoutButtonActionPerformed(evt);
            }
        });

        deleteScheduledLogoutButton.setText("Elimina");
        deleteScheduledLogoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteScheduledLogoutButtonActionPerformed(evt);
            }
        });

        scheduledLogoutList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "prova" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        scheduledLogoutList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(scheduledLogoutList);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(deleteScheduledLogoutButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addScheduledLogoutButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(addScheduledLogoutButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteScheduledLogoutButton)
                        .addGap(0, 172, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane4.addTab("Auto-logout", jPanel2);

        applyButton.setText("Applica");
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(applyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane4))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(applyButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
        String serialPortToChange = "";
        if(serialPortComboBox.getSelectedItem() != null){
            serialPortToChange = serialPortComboBox.getSelectedItem().toString();
        }
        
        //creates an arrayList of LocalTime
        DefaultListModel model = ((DefaultListModel) scheduledLogoutList.getModel());
        List<LocalTime> scheduledLogouts = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++){
            LocalTime logout = LocalTime.parse(model.get(i).toString());
            scheduledLogouts.add(logout);
        }
        
        testMachine.changeProperties(
                serialPortToChange,
                machineTextField.getText(),
                databaseServerNameTextField.getText(),
                databaseUserTextField.getText(),
                databasePasswordTextField.getText(),
                autostartCheckBox.isSelected(),
                debugCheckBox.isSelected(),
                scheduledLogouts);
        this.dispose();
    }//GEN-LAST:event_applyButtonActionPerformed

    private void populateSerialPortComboBox(){
        //Populate combobox with available serial ports
        SerialPort[] ports = testMachine.getAvailableSerialPorts();
        for (SerialPort port : ports) {
            serialPortComboBox.addItem(port.getSystemPortName());
        }
        for (int i = 0; i < serialPortComboBox.getItemCount(); i++) {
            if (serialPortComboBox.getItemAt(i).toString().equals(testMachine.getSerialPortString())) {
                serialPortComboBox.setSelectedIndex(i);
            }
        }
    }
    
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
        parentForm.setEnabled(true);
    }//GEN-LAST:event_formWindowClosed

    private void serialPortComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_serialPortComboBoxPopupMenuWillBecomeVisible
        // Empties combobox
        for (int i = serialPortComboBox.getItemCount() - 1; i >= 0; i--){
            serialPortComboBox.removeItemAt(i);
        }
        populateSerialPortComboBox();
    }//GEN-LAST:event_serialPortComboBoxPopupMenuWillBecomeVisible

    private void addScheduledLogoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addScheduledLogoutButtonActionPerformed
        // TODO add item
        //((DefaultListModel) scheduledLogoutList.getModel()).addElement("Orario");
        
        String s = (String) JOptionPane.showInputDialog("Inserisci un orario.\nUsa un formato del tipo hh:mm");
        if (s != null && !s.equals("")){
            try{
                LocalTime logout = LocalTime.parse(s);
                ((DefaultListModel) scheduledLogoutList.getModel()).addElement(logout);
            }
            catch(java.time.format.DateTimeParseException ex){
                JOptionPane.showMessageDialog(this, "Formato ora non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
                
            }
        }
    }//GEN-LAST:event_addScheduledLogoutButtonActionPerformed

    private void deleteScheduledLogoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteScheduledLogoutButtonActionPerformed
        // chek if this works
        ((DefaultListModel) scheduledLogoutList.getModel()).remove(scheduledLogoutList.getSelectedIndex());
        
    }//GEN-LAST:event_deleteScheduledLogoutButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addScheduledLogoutButton;
    private javax.swing.JButton applyButton;
    private javax.swing.JCheckBox autostartCheckBox;
    private javax.swing.JTextField databasePasswordTextField;
    private javax.swing.JTextField databaseServerNameTextField;
    private javax.swing.JTextField databaseUserTextField;
    private javax.swing.JCheckBox debugCheckBox;
    private javax.swing.JButton deleteScheduledLogoutButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTextField machineTextField;
    private javax.swing.JList<String> scheduledLogoutList;
    private javax.swing.JComboBox<String> serialPortComboBox;
    // End of variables declaration//GEN-END:variables
}
