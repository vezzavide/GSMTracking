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
import org.mindrot.jbcrypt.BCrypt;

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
    private boolean debug;
    private long readTimeOut = 1000;
    private boolean autostart;
    private List<LocalTime> scheduledLogouts;
    Timer logoutTimer = new Timer();
    private boolean loggedIn = false;
    private String nextLogoutString;
    
    public TestMachine(){
        // TODO: load settings
        logger = Logger.getLogger(TestMachine.class.getName());
        
        try {
            fileHandler = new FileHandler(workingDirectory + File.separator + "test_machine_errors.log", true);
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
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
            databaseServerName = properties.getProperty("databaseServerName", "localhost");
            databaseUser = properties.getProperty("databaseUser", "application");
            databasePassword = properties.getProperty("databasePassword", "12Application");
            autostart = Boolean.valueOf(properties.getProperty("autostart", "True"));
            debug = Boolean.valueOf(properties.getProperty("debug", "False"));
            
            String scheduledLogoutsString = properties.getProperty("scheduledLogouts", "");
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
        
        logIfDebugging("Nuova istanza di TestMachine");
    }
    
    public void addListener(TestMachineListener toAdd){
        listeners.add(toAdd);
    }
    
    // EVENT
    public void generateDataPacketReceivedEvent(Board board){
        for (TestMachineListener packetListener : listeners){
            packetListener.dataPacketReceivedEvent(board);
        }
        logIfDebugging("Inviato a GUI evento di pacchetto ricevuto");
    }
    
    // EVENT
    public void generateDisplayErrorEvent(String message){
        for (TestMachineListener packetListener : listeners){
            packetListener.displayError(message);
        }
        logIfDebugging("Inviato a GUI evento di display error");
    }
    
    // EVENT
    public void generateErrorStateEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.errorStateEvent();
        }
        logIfDebugging("Inviato a GUI evento di stato di errore");
    }
    
    // EVENT
    public void generateNormalStateEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.normalStateEvent();
        }
        logIfDebugging("Inviato a GUI evento di stato normale");
    }
    
    // EVENT
    public void generateBoardNotGoodEvent(){
        for (TestMachineListener packetListener : listeners){
            packetListener.boardNotGoodEvent();
        }
        logIfDebugging("Inviato a GUI evento di scheda non good");
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
    
    // EVENT
    public void generateLogoutScheduledEvent(String logout){
        for (TestMachineListener packetListener : listeners){
            packetListener.logoutScheduledEvent(logout);
        }
    }
    
    // EVENT
    public void generateServerStateEvent(int serverState){
        for (TestMachineListener packetListener : listeners){
            packetListener.serverStateEvent(serverState);
        }
    }
    
    public String getNextLogoutString(){
        return nextLogoutString;
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
    
    private void logIfDebugging(String message){
        if(debug){
            logger.log(Level.SEVERE, message);
        }
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
        logIfDebugging("Inizio a fissare il prossimo logout");
        // Deletes the last scheduled logout
        logoutTimer.cancel();
        // Finds the nearest login in the (future) timeline
        if (scheduledLogouts != null && !scheduledLogouts.isEmpty()){
            logoutTimer = new Timer();
            // Initializes nextLogout with maximum value
            LocalTime nextLogout = LocalTime.MAX;
            LocalTime nextTomorrowLogout = scheduledLogouts.get(0);
            LocalTime now = LocalTime.now();
            for (int i = 0; i < scheduledLogouts.size(); i++) {
                // Selects just future logouts
                LocalTime logout = scheduledLogouts.get(i);
                // Finds the next logout of today
                if (logout.isAfter(now) && logout.isBefore(nextLogout)) {
                    nextLogout = logout;
                }
                if (logout.isBefore(nextTomorrowLogout)){
                    nextTomorrowLogout = logout;
                }
            }
            
            Date nextLogoutDate;
            // The next logout is tomorrow at nextTomorrowLogout hours
            if(nextLogout == LocalTime.MAX){
                // Creates a LocalDateTime for next logout (of tomorrow)
                LocalDateTime nextLogoutDateTime = nextTomorrowLogout.atDate(LocalDate.now().plusDays(1));
                // Converts LocalDateTime to date (from java.util), since Timer.schedule supports Date only
                nextLogoutDate = Date.from(nextLogoutDateTime.atZone(ZoneId.systemDefault()).toInstant());
                nextLogoutString = "domani alle " + nextTomorrowLogout.toString();
            }
            // The next logout is today at nextLogout hours
            else{
                // Creates a LocalDateTime for next logout
                LocalDateTime nextLogoutDateTime = nextLogout.atDate(LocalDate.now());
                // Converts LocalDateTime to date (from java.util), since Timer.schedule supports Date only
                nextLogoutDate = Date.from(nextLogoutDateTime.atZone(ZoneId.systemDefault()).toInstant());
                nextLogoutString = nextLogout.toString();
            }
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
                //nextLogoutString = nextLogout.toString();
        }
        else{
            nextLogoutString = "nessuno";
        }
        generateLogoutScheduledEvent(nextLogoutString);
        logIfDebugging("Next logout scheduled");
    }
    
    public boolean newUser(String name, String surname, String username, String password){
        logIfDebugging("Inizio a creare un nuovo utente");
        // TODO implement hashing and salting
        try{
            Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            String capitalizedSurname = surname.substring(0, 1).toUpperCase() + surname.substring(1).toLowerCase();
            // NOTE: hashedPassword contains both salt hand hashed salted password,
            // all handled by BCrypt algorithm
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            //Insert new user
            String query = "INSERT INTO tracking_system.user VALUES (default, '"
                    + capitalizedName
                    + "', '"
                    + capitalizedSurname
                    + "', '"
                    + username
                    + "', '"
                    + hashedPassword
                    + "', '0');";
            statement.executeUpdate(query);
            //System.out.println("Query: " + query);
            logIfDebugging("Nuovo utente creato");
            return true;
        }
        catch(com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ex){
            generateDisplayErrorEvent("Errore: username già in uso!");
            return false;
        }
        catch(SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public boolean authenticateAdmin(String username, String password){
        logIfDebugging("Rischiesta di autenticazione account amministratore ricevuta");
        try(Connection conn = dataSource.getConnection();
                Statement statement = conn.createStatement()){
            // Verifies admin data
            String query = "SELECT user.password, user.admin FROM tracking_system.user "
                    + "WHERE username='"
                    + username
                    + "';";
            ResultSet resultSet = statement.executeQuery(query);
            //Check if the result set is empty
            if(!resultSet.next()){
                generateDisplayErrorEvent("Username inesistente!");
                return false;
            }
            boolean isAdmin = resultSet.getBoolean("admin");
            if(!isAdmin){
                generateDisplayErrorEvent("Questo utente non ha i privilegi di amministratore.");
                return false;
            }
            String sentHashedAdminPassword = resultSet.getString("password");
            if(BCrypt.checkpw(password, sentHashedAdminPassword)){
                logIfDebugging("Amministraotre autenticato");
                return true;
            }
            else{
                generateDisplayErrorEvent("Password utente amministratore sbagliata!");
                return false;
            }
        }
        catch(SQLException ex){
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public boolean changePasswordWithAdmin(
            String adminUsername,
            String adminPassword,
            String username,
            String newPassword){
        logIfDebugging("Inizio cambio password con aiuto amministratore");
        try(Connection conn = dataSource.getConnection();
                Statement statement = conn.createStatement()){
            // Verifies admin data
            String query = "SELECT user.password FROM tracking_system.user "
                    + "WHERE username='"
                    + adminUsername
                    + "';";
            ResultSet resultSet = statement.executeQuery(query);
            //Check if the result set is empty
            if(!resultSet.next()){
                generateDisplayErrorEvent("Username amministratore inesistente!");
                return false;
            }
            
            String sentHashedAdminPassword = resultSet.getString("password");
            
            if(BCrypt.checkpw(adminPassword, sentHashedAdminPassword)){
                // admin password ok, changes password for user username
                String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                query = "UPDATE tracking_system.user "
                        + "SET password = '"
                        + newHashedPassword
                        + "' "
                        + "WHERE username = '"
                        + username
                        + "';";
                statement.executeUpdate(query);
                logIfDebugging("Password cambiata!");
                return true;
            }
            else{
                generateDisplayErrorEvent("Password utente amministratore sbagliata!");
                return false;
            }
            
        }
        catch(SQLException ex){
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public boolean changePassword(
            String username,
            String oldPassword,
            String newPassword){
        logIfDebugging("Inizio cambio password con oldPassword");
        try (Connection conn = dataSource.getConnection();
                Statement statement = conn.createStatement()) {
            // Gets user data
            String query = "SELECT user.password FROM tracking_system.user "
                    + "WHERE username='"
                    + username
                    + "';";
            ResultSet resultSet = statement.executeQuery(query);
            //Checks if the result set is empty (returns false in that case)
            if(!resultSet.next()){
                conn.close();
                statement.close();
                generateDisplayErrorEvent("Username inesistente!");
                return false;
            }
            String sentHashedPassword = resultSet.getString("password");
            
            // Checks old password
            if(BCrypt.checkpw(oldPassword, sentHashedPassword)){
                String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                query = "UPDATE tracking_system.user "
                        + "SET password = '"
                        + newHashedPassword
                        + "' "
                        + "WHERE username = '"
                        + username
                        + "';";
                statement.executeUpdate(query);
                logIfDebugging("Password cambiata");
                return true;
            }
            else{
                generateDisplayErrorEvent("Vecchia password errata!");
                return false;
            }
        }
        catch(SQLException ex){
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public boolean login(String username, String password){
        logIfDebugging("Nuovo tentativo di login");
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
            // sentPassword contains salt AND hashed salted password,
            // everything in the same row, all handled by BCrypt
            String sentPassword = resultSet.getString("password");
            String sentName = resultSet.getString("name");
            String sentSurname = resultSet.getString("surname");
            String sentUsername = resultSet.getString("username");
            boolean sentAdmin = resultSet.getBoolean("admin");
            conn.close();
            statement.close();
            
            //Check hashed and salted password
            if(BCrypt.checkpw(password, sentPassword)){
                // if this point is reached, login has been succesful
                // Now creates new login on server
                conn = dataSource.getConnection();
                statement = conn.createStatement();
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
                
                user = new User(sentName, sentSurname, sentUsername, sentAdmin, sentLoginID);
                //generateLoginOccurredEvent();
                
                // Since a login occurred, if autostart is enabled, the program starts
                if (autostart) {
                    connect();
                }
                loggedIn = true;
                scheduleNextLogout();
                generateLoginOccurredEvent();
                logIfDebugging("Login andato a buon fine");
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
        catch(java.lang.IllegalArgumentException ex){
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore:"
                                        + System.lineSeparator()
                                        + ex.toString());
            return false;
        }
    }
    
    public void logout(){
        logIfDebugging("Logout in corso");
        disconnect();
        user = null;
        loggedIn = false;
        generateLogoutOccurredEvent();
    }
    
    public SerialPort[] getAvailableSerialPorts(){
        return SerialPort.getCommPorts();
    }
    
    private void flushSerialPort(){
        logIfDebugging("Inizio il flush serial port");
        int lengthToFlush = serialPort.bytesAvailable();
        byte[] flushBuffer = new byte[lengthToFlush];
        serialPort.readBytes(flushBuffer, lengthToFlush);
        logIfDebugging("Finito il flush serial port");
    }
    
    
    public void closePort(){
        if ((serialPort != null) && serialPort.isOpen()){
            serialPort.closePort();
        }
    }
    
    public boolean connect() {
        logIfDebugging("Tento apertura porta seriale");
        try {
            if(serialPort.openPort()){
                serialPortJustStarted = true;
                serialPort.openPort();
                createSerialPortListener();
                logIfDebugging("Apertura porta seriale riuscita");
                connected = true;
                generateConnectedEvent();
                return true;
            }
            else{
                //System.out.println("No porta presente");
                generateDisplayErrorEvent("La porta seriale di default non è più presente."
                + System.lineSeparator() + "Selezionarne una nuova tra le disponibili nel menu impostazioni.");

                logIfDebugging("Apertura porta seriale non riuscita");
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
            
            logIfDebugging("Apertura porta seriale non riuscita");
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
        
        // Resets state variables
        connected = false;
        errorState = false;
        readyToGoodToGoSignal = false;
        serialPortJustStarted = false;
        generateDisconnectedEvent();
    }
    
    public void sendGoodToGoSignal() {
        logIfDebugging("Inizio a inviare il segnale di via libera");
        flushSerialPort();
        serialPort.writeBytes("G".getBytes(), 1);
        logIfDebugging("Segnale di via libera inviato");
    }
    
    public void sendReScanSignal() {
        logIfDebugging("Inizio a inviare il segnale di rescan");
        flushSerialPort();
        serialPort.writeBytes("S".getBytes(), 1);
        logIfDebugging("Segnale di rescan inviato");
    }
    
    private boolean readDataPacketInTimeoutMode(){
        try{
            logIfDebugging("Inizio lettura data packet");
            // reset error counter in case there have actually been errors
            errorRecoveryAttemptsCounter = 0;
            // Starts timer for readign timeout
            long startReadingTime = System.currentTimeMillis();
            logIfDebugging("Comincio ad aspettare che siano ricevuti 27 byte");
            while(serialPort.bytesAvailable() < 27){
                /*wait for whole packet to be received:
                    - 1 TAB;
                    - 24 characters (code);
                    - 1 TAB;
                    - 1 character for goodOrNot;
                        TOT: 27 bytes
                */
                if((System.currentTimeMillis() - startReadingTime) > readTimeOut){
                    logIfDebugging("Timeout data packet scaduto, non ho ricevuto abbastanza dati");
                    return false;
                }
            }
            logIfDebugging("Ho ricevuto almeno 27 byte");
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
            logIfDebugging("Ho finito di leggere il data packet. la scheda ricevuta è: " + currentBoard.getCode());
            if (currentBoard.check()){
                generateDataPacketReceivedEvent(currentBoard);
                return true;
            }
            else{
                return false;
            }
        }
        catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
            logIfDebugging("Si è verificata una eccezione mentre ricevevo e leggevo il datapacket");
            return false;
        }
    }
    
    private Integer readErrorPacketInTimeoutMode(){
        try{
            logIfDebugging("Comincio a leggere l'error packet");
            long startReadingTime = System.currentTimeMillis();
            logIfDebugging("Comincio ad aspettare che siano ricevuto 2 byte");
            while(serialPort.bytesAvailable() < 2){
                // Just wait for whole errore paket to be received:
                //      - 1 TAB
                //      - 1 char (error code)
                //          TOT: 2 bytes

                if((System.currentTimeMillis() - startReadingTime) > readTimeOut){
                    logIfDebugging("Timeout error packet scaduto, non ho ricevuto abbastanza dati");
                    return null;
                }
            }
            logIfDebugging("Sono stati ricevuti abbastanza dati");
            byte[] byteCharacter = new byte[2];
            serialPort.readBytes(byteCharacter, 2);
            Character character = (char)byteCharacter[1];
            String error = character.toString();
            int errorCode = Integer.parseInt(error);
            //System.out.println("Errore da Arduino: codice " + error);
            logIfDebugging("Ho finito di ricevere l'error packet, errore " + error);
            return errorCode;
        }
        catch(Exception ex){
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    private void handleArduinoError(int errorCode){
        logIfDebugging("Comincio a gestire un errore arduino");
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
                            + "- scanner posizionato male"
                            + System.lineSeparator()
                            + "- etichetta rovinata o non presente, controllare la scheda"
                            + System.lineSeparator()
                            + "- vetro dello scanner sporco"
                            + System.lineSeparator()
                            + "- scanner spento"
                            + System.lineSeparator()
                            + "- connessione fisica tra Arduino e scanner compromessa");
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
                    generateDisplayErrorEvent("Codice errore sconosciuto, probabile disturbo sulla linea USB");
                    break;
            }
            errorState = true;
            generateErrorStateEvent();
            logIfDebugging("Ho finito di gestire errore arduino");
        }
    }
    
    private boolean sendCurrentPacketToDB(){
        logIfDebugging("Comincio l'operazione di invio pacchetto al DB");
        // Signals that we start the insert to server
        generateServerStateEvent(1);
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
            
            logIfDebugging("Provo a inviare il test al DB");
            // INSERT for tracking_system.test table
            query = "INSERT INTO tracking_system.test "
                    + "VALUES (default, default, '"
                    + currentBoard.getCode()
                    + "', '"
                    + currentBoard.isGoodString()
                    + "', '"
                    + user.getLoginIDToString()
                    + "');";
            //System.out.println("Eseguo query: " + query);
            statement.executeUpdate(query);
            conn.close();
            statement.close();
            logIfDebugging("Test inviato al DB con successo");
            // Signals that everything went alright
            generateServerStateEvent(2);
            return true;

        }catch (SQLException ex) {
            logIfDebugging("Errore nell'inviare il test al DB");
            logger.log(Level.SEVERE, null, ex);
            generateDisplayErrorEvent("Errore dal server: "
                                    + System.lineSeparator()
                                    + ex.toString());
            // Signals that there has been an error
            generateErrorStateEvent();
            // Signals that something went wrong with the server
            generateServerStateEvent(3);
            return false;
        }
    }
    
    private void createSerialPortListener(){
        logIfDebugging("Creo un nuovo serialPortListener");
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            @Override
            public void serialEvent(SerialPortEvent event)
            {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE){
                    return;
                }
                logIfDebugging("Evento dati ricevuti su porta seriale generato");
                byte[] headerInByte = new byte[1];
                serialPort.readBytes(headerInByte, 1);
                String header = new String(headerInByte);
                logIfDebugging("Letto il primo byte");
                
                switch (header) {
                    case "D":
                        logIfDebugging("E' stato ricevuto un header data packet");
                        serialPortJustStarted = false;
                        readyToGoodToGoSignal = false;
                        if (errorState){
                            errorState = false;
                            generateNormalStateEvent();
                        }
                        if(readDataPacketInTimeoutMode()){
                            if(sendCurrentPacketToDB()){
                                if (currentBoard.isGood()){
                                    logIfDebugging("La scheda è buona, si può mandare il segnale di via libera, metto il flag readyToGoodToGoSignal a 1");
                                    readyToGoodToGoSignal = true;
                                }
                                else{
                                    logIfDebugging("La scheda non è buona, genero evento di scheda non buona E  METTO IL FLAG readyToGoodToGoSignal A 1");
                                    generateBoardNotGoodEvent();
                                    // readyToGoodToGoSignal enabled because of new specification:
                                    // application needs to send good to go EVEN when board is not good.
                                    readyToGoodToGoSignal = true;
                                }
                            }
                            else{
                                logIfDebugging("l'invio pacchetto al DB  non è andato a buon fine.");
                            }
                            logIfDebugging("Ho finito di leggere il data packet");
                        }
                        else{
                            logIfDebugging("lettura pacchetto dati ha ritornato false");
                            //sendReScanSignal();
                            errorState = true;
                            generateErrorStateEvent();
                            generateDisplayErrorEvent("Probabile disturbo sulla linea USB.\n");
                        }
                        flushSerialPort();
                        logIfDebugging("Finito di gestire l'header data packet");
                        break;
                    case "E":
                        logIfDebugging("è stato ricevuto un header error packet");
                        serialPortJustStarted = false;
                        readyToGoodToGoSignal = false;
                        Integer errorCode = readErrorPacketInTimeoutMode();
                        // check if timeout occurred
                        if (errorCode == null){
                            logIfDebugging("Errore di ricezione error packet, riscansiono");
                            sendReScanSignal();
                            break;
                        }
                        handleArduinoError(errorCode);
                        flushSerialPort();
                        logIfDebugging("finito di gestire l'header error packet");
                        break;
                    case "G":
                        logIfDebugging("è stato ricevuto un header request good to go packet");
                        if(serialPortJustStarted){
                            logIfDebugging("Riscansiono perchè la porta seriale è stata appena accesa");
                            sendReScanSignal();
                        }
                        if(readyToGoodToGoSignal){
                            logIfDebugging("Il flag readyToGoodToGoSignal era a 1, quindi invia il segnale di goodToGo");
                            sendGoodToGoSignal();
                        }
                        flushSerialPort();
                        logIfDebugging("finito di gestire l'header G");
                        break;
                    default:
                        logIfDebugging("STRANO: sono nel caso defaut nello switch degli header nell'evento seriale!");
                        break;
                }
            }
        });
    }
}
