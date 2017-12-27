/*
 *  TestMachine application logic
 */
package it.gsm.trackingsystem.testmachine;
import java.util.*;
import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Properties;
import java.time.*;

/**
 *
 * @author abdhul
 */
public class TestMachine {
    private List<TestMachineListener> listeners = new ArrayList<TestMachineListener>();
    private SerialPort serialPort;
    private Board currentBoard;
    private User user;
    private String machine;
    
    // State variables
    private boolean errorState = false;
    private boolean readyToGoodToGoSignal = false;
    private boolean serialPortJustStarted = true;
    private boolean connected = false;
    
    private FileHandler fileHandler;
    private Logger logger;
    private String workingDirectory = System.getProperty(("user.dir"));
    private int maxErrorRecoveryAttempts = 1;
    private int errorRecoveryAttemptsCounter = 0;
    private String databaseUser;
    private String databasePassword;
    private String databaseServerName;
    private MysqlDataSource dataSource;
    private boolean debug = true;
    private long readTimeOut = 1000;
    private boolean autostart = false;
    private List<LocalTime> scheduledLogouts;
    Timer logoutTimer = new Timer();
    private boolean loggedIn = false;
    
    public TestMachine(){
        // TODO: load settings
        logger = Logger.getLogger(TestMachine.class.getName());
        
        try {
            fileHandler = new FileHandler(workingDirectory + File.separator + "test_machine_errors.log", true);
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        /*
        databaseUser = "application";
        databasePassword = "12Application";
        databaseServerName = "localhost";
        machine = "A";
        */
        dataSource = new MysqlDataSource();
        
        try{
            Properties properties = new Properties();
            String propertiesPath = workingDirectory + File.separator + "TestMachine.conf";
            FileInputStream in = new FileInputStream(propertiesPath);
            properties.load(in);
            in.close();
            serialPort = SerialPort.getCommPort(properties.getProperty("serialPort", null));
            //System.out.println("porta scelta: " + serialPort.getSystemPortName());
            machine = properties.getProperty("machine");
            databaseServerName = properties.getProperty("databaseServerName");
            databaseUser = properties.getProperty("databaseUser");
            databasePassword = properties.getProperty("databasePassword");
            autostart = Boolean.valueOf(properties.getProperty("autostart"));
            debug = Boolean.valueOf(properties.getProperty("debug"));
            
            String scheduledLogoutsString = properties.getProperty("scheduledLogouts");
            if(!scheduledLogoutsString.equals("")){
                String[] logoutsSplit = scheduledLogoutsString.split("#");
                List<LocalTime> scheduledLogouts = new ArrayList<>();
                for (String logout : logoutsSplit){
                    scheduledLogouts.add(LocalTime.parse(logout));
                }
                this.scheduledLogouts = scheduledLogouts;
            }
            
            dataSource.setUser(databaseUser);
            dataSource.setPassword(databasePassword);
            dataSource.setServerName(databaseServerName);
        }
        catch(FileNotFoundException ex){
            generateDisplayErrorEvent("Errore nel caricare le impostazioni! Inserirle manualmente.");
        }
        catch(IOException ex){
            generateDisplayErrorEvent("Errore nel caricare le impostazioni! Inserirle manualmente.");
        }
        
        if (debug){
            logger.log(Level.SEVERE, "Nuova istanza di TestMachine");
        }
    }
    
    public void addListener(TestMachineListener toAdd){
        listeners.add(toAdd);
    }
    
    // EVENT
    public void generateDataPacketReceivedEvent(Board board){
        for (TestMachineListener packetListener : listeners){
            packetListener.dataPacketReceivedEvent(board);
        }
        if (debug){
            logger.log(Level.SEVERE, "Inviato a GUI evento di pacchetto ricevuto");
        }
    }
    
    // EVENT
    public void generateDisplayErrorEvent(String message){
        for (TestMachineListener packetListener : listeners){
            packetListener.displayError(message);
        }
        if (debug){
            logger.log(Level.SEVERE, "Inviato a GUI evento di display error");
        }
    }
    
    // EVENT
    public void generateErrorStateEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.errorStateEvent();
        }
        if (debug){
            logger.log(Level.SEVERE, "Inviato a GUI evento di stato di errore");
        }
    }
    
    // EVENT
    public void generateNormalStateEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.normalStateEvent();
        }
        if (debug){
            logger.log(Level.SEVERE, "Inviato a GUI evento di stato normale");
        }
    }
    
    // EVENT
    public void generateBoardNotGoodEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.boardNotGoodEvent();
        }
        if (debug){
            logger.log(Level.SEVERE, "Inviato a GUI evento di scheda non good");
        }
    }
    
    // EVENT
    public void generateLoginOccurredEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.loginOccurredEvent(user);
        }
    }
    
    // EVENT
    public void generateConnectedEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.connectedEvent();
        }
    }
    
    // EVENT
    public void generateDisconnectedEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.disconnectedEvent();
        }
    }
    
    // EVENT
    public void generateLogoutOccurredEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.logoutOccurredEvent();
        }
    }
    
    public boolean isConnected(){
        return connected;
    }
    
    public String getSerialPortString(){
        if (serialPort != null){
            return serialPort.getSystemPortName();
        }
        else{
            return null;
        }
    }
    
    public List<LocalTime> getScheduledLogouts(){
        return scheduledLogouts;
    }
    
    public String getMachine(){
        return machine;
    }
    
    public String getDatabaseServerName(){
        return databaseServerName;
    }
    
    public String getDatabaseUser(){
        return databaseUser;
    }
    
    public String getDatabasePassword(){
        return databasePassword;
    }
    
    public boolean isAutostartEnabled(){
        return autostart;
    }
    
    public boolean isDebugEnabled(){
        return debug;
    }
    
    public void changeProperties(
            String serialPort,
            String machine,
            String databaseServerName,
            String databaseUser,
            String databasePassword,
            boolean autostart,
            boolean debug,
            List<LocalTime> scheduledLogouts){
        
        // Saves properties
        Properties properties = new Properties();
        String propertiesPath = workingDirectory + File.separator + "TestMachine.conf";
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(propertiesPath);
        } catch (FileNotFoundException ex) {
            File f = new File(propertiesPath);
            f.getParentFile().mkdirs();
            try {
                f.createNewFile();
            } catch (IOException ex1) {
                Logger.getLogger(TestMachine.class.getName()).log(Level.SEVERE, null, ex1);
            }
            try {
                out = new FileOutputStream(propertiesPath);
            } catch (FileNotFoundException ex1) {
                Logger.getLogger(TestMachine.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        properties.setProperty("serialPort", serialPort);
        properties.setProperty("machine", machine);
        properties.setProperty("databaseServerName", databaseServerName);
        properties.setProperty("databaseUser", databaseUser);
        properties.setProperty("databasePassword", databasePassword);
        Boolean autostartBoolean = autostart;
        properties.setProperty("autostart", autostartBoolean.toString());
        Boolean debugBoolean = debug;
        properties.setProperty("debug", debugBoolean.toString());
        
        String scheduledLogoutsString = "";
        for (LocalTime logout : scheduledLogouts){
            scheduledLogoutsString += logout.toString() + "#";
        }
        properties.setProperty("scheduledLogouts", scheduledLogoutsString);
        
        try {
            properties.store(out, "---Config file for TestMachine application---");
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(TestMachine.class.getName()).log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore nel salvare le impostazioni.");
        }
        
        // Actually changes properties
        this.serialPort = SerialPort.getCommPort(serialPort);
        this.machine = machine;
        this.databaseServerName = databaseServerName;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.autostart = autostart;
        this.debug = debug;
        dataSource.setUser(databaseUser);
        dataSource.setPassword(databasePassword);
        dataSource.setServerName(databaseServerName);
        this.scheduledLogouts = scheduledLogouts;
        scheduleNextLogout();
    }
    
    private void scheduleNextLogout(){
        // Deletes the last scheduled logout
        logoutTimer.cancel();
        // Finds the nearest login in the (future) timeline
        if (scheduledLogouts != null){
            logoutTimer = new Timer();
            // Initializes nextLogout with maximum value
            LocalTime nextLogout = LocalTime.MAX;
            LocalTime now = LocalTime.now();
            for (int i = 0; i < scheduledLogouts.size(); i++) {
                // Selects just future logouts
                LocalTime logout = scheduledLogouts.get(i);
                if (logout.isAfter(now) && logout.isBefore(nextLogout)) {
                    nextLogout = logout;
                }
            }
            
            // Creates a LocalDateTime for next logout
            LocalDateTime nextLogoutDateTime = nextLogout.atDate(LocalDate.now());
            // Converts LocalDateTime to date (from java.util), since Timer.schedule supports Date only
            Date nextLogoutDate = Date.from(nextLogoutDateTime.atZone(ZoneId.systemDefault()).toInstant());
            
            // At this point, nextLogoutDate is the nearest future logout in the list
            logoutTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        logout();
                        logoutTimer.cancel();
                    }
                },
                nextLogoutDate);
        }
    }
    
    public boolean newUser(String name, String surname, String username, String password){
        try{
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            String capitalizedSurname = surname.substring(0, 1).toUpperCase() + surname.substring(1).toLowerCase();
            
            //Insert new user
            String query = "INSERT INTO tracking_system.user VALUES (default, '"
                    + capitalizedName
                    + "', '"
                    + capitalizedSurname
                    + "', '"
                    + username
                    + "', '"
                    + password
                    + "', '0');";
            statement.executeUpdate(query);
            System.out.println("Query: " + query);
            return true;
        }
        catch(SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
        //return true;
    }
    
    public boolean login(String username, String password){
        try{
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            /* example query:
                INSERT INTO tracking_system.test
                VALUES (default, default, 'codicescheda', 'goodornot', 'username', 'macchina', 'login_id');
            */
            String query = "SELECT * FROM tracking_system.user "
                    + "WHERE username='"
                    + username
                    + "';";
            ResultSet resultSet = statement.executeQuery(query);
            //Check if the result set is empty (returns false in that case)
            if(!resultSet.next()){
                conn.close();
                statement.close();
                generateDisplayErrorEvent("Username inesistente!");
                return false;
            }
            String sentPassword = resultSet.getString("password");
            String sentName = resultSet.getString("name");
            String sentSurname = resultSet.getString("surname");
            String sentUsername = resultSet.getString("username");
            boolean sentAdmin = resultSet.getBoolean("admin");
            
            // Now creates new login on server
            query = "INSERT INTO tracking_system.login "
                    + "VALUES (default, '"
                    + sentUsername
                    + "', default, '"
                    + machine
                    + "');";
            statement.executeUpdate(query);
            
            // Now gets last incremented ID
            query = "SELECT LAST_INSERT_ID();";
            resultSet = statement.executeQuery(query);
            resultSet.next();
            int sentLoginID = resultSet.getInt("LAST_INSERT_ID()");
            //System.out.println("loginID ottenuto: " + sentLoginID);
            
            conn.close();
            statement.close();
            
            if(sentPassword.equals(password)){
                // if this point is reached, login has been succesful
                user = new User(sentName, sentSurname, sentUsername, sentAdmin, sentLoginID);
                generateLoginOccurredEvent();
                
                // Since a login occurred, if autostart is enabled, the program starts
                if (autostart) {
                    connect();
                }
                loggedIn = true;
                scheduleNextLogout();
                return true;
            }
            else{
                generateDisplayErrorEvent("Password sbagliata!");
                return false;
            }
        }
        catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public void logout(){
        disconnect();
        user = null;
        loggedIn = false;
        generateLogoutOccurredEvent();
    }
    
    public SerialPort[] getAvailableSerialPorts(){
        return SerialPort.getCommPorts();
    }
    
    private void flushSerialPort(){
        if (debug){
            logger.log(Level.SEVERE, "Inizio il flush serial port");
        }
        int lengthToFlush = serialPort.bytesAvailable();
        byte[] flushBuffer = new byte[lengthToFlush];
        serialPort.readBytes(flushBuffer, lengthToFlush);
        if (debug){
            logger.log(Level.SEVERE, "Finito il flush serial port");
        }
    }
    
    
    public void closePort(){
        if ((serialPort != null) && serialPort.isOpen()){
            serialPort.closePort();
        }
    }
    
    public boolean connect() {
        if (debug){
            logger.log(Level.SEVERE, "Tento apertura porta seriale");
        }
        try {
            if(serialPort.openPort()){
                serialPortJustStarted = true;
                serialPort.openPort();
                createSerialPortListener();

                if (debug){
                    logger.log(Level.SEVERE, "Apertura porta seriale riuscita");
                }   
                connected = true;
                generateConnectedEvent();
                return true;
            }
            else{
                //System.out.println("No porta presente");
                generateDisplayErrorEvent("La porta seriale di default non è più presente."
                + System.lineSeparator() + "Selezionarne una nuova tra le disponibili nel menu impostazioni.");

                if (debug) {
                    logger.log(Level.SEVERE, "Apertura porta seriale non riuscita");
                }
                connected = false;
                generateDisconnectedEvent();
                return false;
            }
        } catch (NullPointerException ex) {
            //connectButton.setText("null object returned!");
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dalla porta seriale: "
                                    + System.lineSeparator()
                                    + ex.toString());
            
            if (debug){
                logger.log(Level.SEVERE, "Apertura porta seriale non riuscita");
            }
            connected = false;
            generateDisconnectedEvent();
            return false;
        }
    }
    
    public void disconnect(){
        try {
            //connectButton.setText(this.getSelectedSerialPort().getSystemPortName());
            serialPort.closePort();
            serialPort.removeDataListener();
        } catch (NullPointerException ex) {
            //
        }
        connected = false;
        generateDisconnectedEvent();
    }
    
    public void sendGoodToGoSignal() {
        if (debug){
            logger.log(Level.SEVERE, "Inizio a inviare il segnale di via libera");
        }
        flushSerialPort();
        serialPort.writeBytes("G".getBytes(), 1);
        if (debug){
            logger.log(Level.SEVERE, "Segnale di via libera inviato");
        }
    }
    
    public void sendReScanSignal() {
        if (debug){
            logger.log(Level.SEVERE, "Inizio a inviare il segnale di rescan");
        }
        flushSerialPort();
        serialPort.writeBytes("S".getBytes(), 1);
        if (debug){
            logger.log(Level.SEVERE, "Segnale di rescan inviato");
        }
    }
    
    private boolean readDataPacketInBlockingMode(){
        if (debug){
            logger.log(Level.SEVERE, "Inizio lettura data packet");
        }
        // reset error counter in case there have actually been errors
        errorRecoveryAttemptsCounter = 0;
        // Starts timer for readign timeout
        long startReadingTime = System.currentTimeMillis();
        if (debug){
            logger.log(Level.SEVERE, "Comincio ad aspettare che siano ricevuti 27 byte");
        }
        while(serialPort.bytesAvailable() < 27){
            /*wait for whole packet to be received:
                - 1 TAB;
                - 24 characters (code);
                - 1 TAB;
                - 1 character for goodOrNot;
                    TOT: 27 bytes
            */
            if((System.currentTimeMillis() - startReadingTime) > readTimeOut){
                if (debug){
                    logger.log(Level.SEVERE, "Timeout data packet scaduto, non ho ricevuto abbastanza dati");
                }
                return false;
            }
        }
        if (debug){
            logger.log(Level.SEVERE, "Ho ricevuto almeno 27 byte");
        }
        currentBoard = new Board();
        byte[] byteCharacter = new byte[27];
        serialPort.readBytes(byteCharacter, 27);
        String packet = new String(byteCharacter);
        for (int i = 1; i <= 24; i++){
            currentBoard.append(packet.charAt(i));
        }
        if (packet.charAt(26) == '0'){
            currentBoard.setGood(false);
        } else{
            currentBoard.setGood(true);
        }
        //updatePacketGUI();
        generateDataPacketReceivedEvent(currentBoard);
        if (debug){
            logger.log(Level.SEVERE, "Ho finito di leggere il data packet. la scheda ricevuta è: " + currentBoard.getCode());
        }
        return currentBoard.check();
    }
    
    private Integer readErrorPacketInBlockingMode(){
        if (debug){
            logger.log(Level.SEVERE, "Comincio a leggere l'error packet");
        }
        long startReadingTime = System.currentTimeMillis();
        if (debug){
            logger.log(Level.SEVERE, "Comincio ad aspettare che siano ricevuto 2 byte");
        }
        while(serialPort.bytesAvailable() < 2){
            // Just wait for whole errore paket to be received:
            //      - 1 TAB
            //      - 1 char (error code)
            //          TOT: 2 bytes
            
            if((System.currentTimeMillis() - startReadingTime) > readTimeOut){
                if (debug){
                    logger.log(Level.SEVERE, "Timeout error packet scaduto, non ho ricevuto abbastanza dati");
                }
                return null;
            }
        }
        if (debug){
            logger.log(Level.SEVERE, "Sono stati ricevuti abbastanza dati");
        }
        byte[] byteCharacter = new byte[2];
        serialPort.readBytes(byteCharacter, 2);
        Character character = (char)byteCharacter[1];
        String error = character.toString();
        int errorCode = Integer.parseInt(error);
        //System.out.println("Errore da Arduino: codice " + error);
        if (debug){
            logger.log(Level.SEVERE, "Ho finito di ricevere l'error packet, errore " + error);
        }
        return errorCode;
    }
    
    private void handleArduinoError(int errorCode){
        if (debug){
            logger.log(Level.SEVERE, "Comincio a gestire un errore arduino");
        }
        if (errorRecoveryAttemptsCounter < maxErrorRecoveryAttempts){
            flushSerialPort();
            sendReScanSignal();
            errorRecoveryAttemptsCounter++;
        }
        else {
            errorRecoveryAttemptsCounter = 0;
            switch(errorCode){
                case 1:
                    generateDisplayErrorEvent("Errore E1: Arduino non ha ricevuto"
                            + "dati dallo scanner."
                            + System.lineSeparator()
                            + "Possibili cause:"
                            + System.lineSeparator()
                            + "- connessione fisica tra Arduino e Barcode Scanner compromessa"
                            + System.lineSeparator()
                            + "- barcode Scanner spento"
                            + System.lineSeparator()
                            + "- vetro dello scanner sporco");
                    break;
                case 2:
                    generateDisplayErrorEvent("Errore E2: il codice ricevuto dallo scanner è più corto del previsto."
                            + System.lineSeparator()
                            + "Possibili cause:"
                            + System.lineSeparator()
                            + "- è stato letto un codice sbagliato o con un formato diverso da quello previsto"
                            + System.lineSeparator()
                            + "- disturbi sulla connessione scanner-arduino");
                    break;
                case 3:
                    generateDisplayErrorEvent("Errore E3: il codice ricevuto dallo scanner è in un formato sbagliato."
                            + System.lineSeparator()
                            + "Possibili cause:"
                            + System.lineSeparator()
                            + "- è stato letto un codice sbagliato o con un formato diverso da quello previsto"
                            + System.lineSeparator()
                            + "- disturbi sulla connessione scanner-arduino");
                    break;
                case 4:
                    generateDisplayErrorEvent("Errore E4: il codice ricevuto dallo scanner è più lungo del previsto."
                            + System.lineSeparator()
                            + "Possibili cause:"
                            + System.lineSeparator()
                            + "- è stato letto un codice sbagliato o con un formato diverso da quello previsto"
                            + System.lineSeparator()
                            + "- disturbi sulla connessione scanner-arduino");
                    break;
                default:
                    generateDisplayErrorEvent("Codice errore non conosciuto, probabile disturbo sulla linea USB");
                    break;
            }
            errorState = true;
            generateErrorStateEvent();
            if (debug){
                logger.log(Level.SEVERE, "Ho finito di gestire errore arduino");
            }
        }
    }
    
    private boolean sendCurrentPacketToDB(){
        if (debug){
            logger.log(Level.SEVERE, "Comincio l'operazione di invio pacchetto al DB");
        }
        try {
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            /* example query:
                INSERT INTO tracking_system.test
                VALUES (default, default, 'codicescheda', 'goodornot', 'username', 'macchina', 'login_id);
            */
            String query;
            
            
            try{
                // INSERT for tracking_system.board table
                query = "INSERT INTO tracking_system.board "
                        + "VALUES (default, '"
                        + currentBoard.getCode()
                        + "', default);";

                statement.executeUpdate(query);
            }
            // If board is already present in DB, it's alright (after all,
            //each board can be subjected to many tests)
            catch(com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ex){
                // Does nothing
            }
            
            if (debug){
                logger.log(Level.SEVERE, "Provo a inviare il test al DB");
            }
            // INSERT for tracking_system.test table
            query = "INSERT INTO tracking_system.test "
                    + "VALUES (default, default, '"
                    + currentBoard.getCode()
                    + "', '"
                    + currentBoard.isGoodString()
                    + "', '"
                    + user.getUsername()
                    + "', '"
                    + machine
                    + "', '"
                    + user.getLoginIDToString()
                    + "');";
            //System.out.println("Eseguo query: " + query);
            statement.executeUpdate(query);
            conn.close();
            statement.close();
            if (debug){
                logger.log(Level.SEVERE, "Test inviato al DB con successo");
            }
            return true;

        }catch (SQLException ex) {
            if (debug){
                logger.log(Level.SEVERE, "Errore nell'inviare il test al DB");
            }
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server: "
                                    + System.lineSeparator()
                                    + ex.toString());
            generateErrorStateEvent();
            return false;
        }
    }
    
    private void createSerialPortListener(){
        if (debug){
            logger.log(Level.SEVERE, "Creo un nuovo serialPortListener");
        }
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            @Override
            public void serialEvent(SerialPortEvent event)
            {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE){
                    return;
                }
                
                if (debug){
                    logger.log(Level.SEVERE, "Evento dati ricevuti su porta seriale generato");
                }
                
                byte[] headerInByte = new byte[1];
                serialPort.readBytes(headerInByte, 1);
                String header = new String(headerInByte);
                if (debug){
                    logger.log(Level.SEVERE, "Letto il primo byte");
                }
                
                switch (header) {
                    case "D":
                        if (debug){
                            logger.log(Level.SEVERE, "E' stato ricevuto un header data packet");
                        }
                        serialPortJustStarted = false;
                        readyToGoodToGoSignal = false;
                        if (errorState){
                            errorState = false;
                            generateNormalStateEvent();
                        }
                        if(readDataPacketInBlockingMode()){
                            //System.out.println("pacchetto ricevuto per intero!");
                            if(sendCurrentPacketToDB()){
                                if (currentBoard.isGood()){
                                    if (debug){
                                        logger.log(Level.SEVERE, "La scheda è buona, si può mandare il segnale di via libera, metto il flag readyToGoodToGoSignal a 1");
                                    }
                                    readyToGoodToGoSignal = true;
                                }
                                else{
                                    if (debug){
                                        logger.log(Level.SEVERE, "La scheda non è buona, genero evento di scheda non buona");
                                    }
                                    generateBoardNotGoodEvent();
                                }
                                //System.out.println("pacchetto spedito al db e ora procedo");
                            }
                            else{
                                if (debug){
                                    logger.log(Level.SEVERE, "l'invio pacchetto al DB  non è andato a buon fine, invio segnale di rescan");
                                }
                                sendReScanSignal();
                            }
                            if (debug){
                                logger.log(Level.SEVERE, "Ho finito di leggere il data packet");
                            }
                        }
                        else{
                            if (debug){
                                logger.log(Level.SEVERE, "Timeout di lettura pacchetto dati, riscansiono");
                                sendReScanSignal();
                            }
                        }
                        flushSerialPort();
                        if (debug){
                            logger.log(Level.SEVERE, "Finito di gestire l'header data packet");
                        }
                        break;
                    case "E":
                        if (debug){
                            logger.log(Level.SEVERE, "è stato ricevuto un header error packet");
                        }
                        serialPortJustStarted = false;
                        readyToGoodToGoSignal = false;
                        Integer errorCode = readErrorPacketInBlockingMode();
                        // check if timeout occurred
                        if (errorCode == null){
                            sendReScanSignal();
                            break;
                        }
                        handleArduinoError(errorCode);
                        flushSerialPort();
                        if (debug){
                            logger.log(Level.SEVERE, "finito di gestire l'header error packet");
                        }
                        break;
                    case "G":
                        if (debug){
                            logger.log(Level.SEVERE, "è stato ricevuto un header request good to go packet");
                        }
                        if(serialPortJustStarted){
                            //serialPortJustStarted = false;
                            if (debug){
                                logger.log(Level.SEVERE, "Riscansiono perchè la porta seriale è stata appena accesa");
                            }
                            sendReScanSignal();
                        }
                        if(readyToGoodToGoSignal){
                            if (debug){
                                logger.log(Level.SEVERE, "Il flag readyToGoodToGoSignal era a 1, quindi invia il segnale di goodToGo");
                            }
                            sendGoodToGoSignal();
                        }
                        flushSerialPort();
                        if (debug){
                            logger.log(Level.SEVERE, "finito di gestire l'header G");
                        }
                        break;
                    default:
                        if (debug){
                            logger.log(Level.SEVERE, "STRANO: sono nel caso defaut nello switch degli header nell'evento seriale!");
                        }
                        break;
                }
            }
        });
    }
}
