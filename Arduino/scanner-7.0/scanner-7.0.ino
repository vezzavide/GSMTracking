 /*  
  *  PROJECT:
  *     GSM Tracking System
  *     
  *  DESTINATION OF THIS CODE:
  *     Arduinos that'll be placed next to test machines,
  *     connected directly with barcode scanners via rs232
  *     (set in standard-RS232-only-ASCII mode).
  *     One Arduino for each machine, one scanner each.
  *     Pc will communicate with scanner and machine
  *     through arduino usb cable.
  *  
  *  BUILDING CONSTRAINTS AND NOTES:
  *     - signal goodOrNot is read right after receiving trigger signal
  *       from PLC, so it needs to go HIGH when (or slightly before)
  *       trigger signal is received and it needs to stay HIGH
  *       for a few seconds (1 or 2), just to be sure
  *     - a filter is on place over trigger signal from PLC, which means
  *       that the trigger can be activated by the PLC only every
  *       triggerInFilter milliseconds
  *  
  *  COMMUNICATION PROTOCOL:
  *    - from Arduino to PC:
  *       - if sending data:
  *             D + [TAB] + [CODE] + [TAB] + [goodOrNot] + [\n]
  *       - if sending error:
  *             E + [TAB] + [errorCode] + [\n]
  *       - if communicating goodToGo signal request:
  *             G + [\n]
  *    - from PC to Arduino:
  *       - if asking scan request:
  *             S
  *       - if sending goodToGo signal:
  *             G
  *  
  *  Machine State description (retained by the variable 'state'):
  *  
  *  STATE 0: Waiting for scan signal from PLC and clearing hardware serial
  *           buffer.
  *           If scan signal is received, program goes to state 1.
  *           The program waits for a forced goodToGo signal too: if it
  *           is received, a good to go signal is sent and the program stays
  *           in state 0.
  *           
  *  STATE 1: Enabling scanner's trigger, waiting for transmission
  *           start from scanner and clearing hardware serial buffer.
  *           If nothing is received for triggerTimeOut milliseconds,
  *           an error 1 is thrown and the program goes to state 4.
  *           If the transmission starts (i.e. the first character is
  *           received), the program goes to state 2.
  *           
  *  STATE 2: Receiving data from scanner.
  *           If not enough data is received, an error 2 is thrown and
  *           the program goes to state 4.
  *           If exactly 24 characters are received, the program goes
  *           to state 3.
  *           
  *  STATE 3: Program stays in this state for exactly
  *           overflowCheckTimeOut millisecond, every time this state
  *           is reached.
  *           If no other characters are received during this time,
  *           the code is checked: if ok, it's sent and the program goes
  *           to state 4; if not (i.e. is in a bad format/corrupt),
  *           an error 3 is thrown and the program goes to state 4.
  *           Otherwise, if other characters have actually been
  *           received, it means that an overflow happened: an error
  *           4 is thrown and the program goes to state 4.
  *  
  *  STATE 4: The program waits for a good to go signal from PC
  *           or for a resend signal from PC.
  *           If a resend signal is received, the program goes to
  *           state 1;
  *           instead, if a goodToGo signal is received, goodToGo
  *           signal is sent to PC and the program goes to state 0.
  *           Note that this state is reached every time an error
  *           is thrown and every time a correct reading is sent
  *           from the scanner! At this point, the program on
  *           PC would be able to send a goodToGo signal even after an
  *           error (that's to avoid being stuck in this state if, e.g.,
  *           a recurrent error keeps happening).
  */

////////// TODO:
/*
 *    - overflow of time after 50 days
 *    - if errors happen over time, better drop string object
 *      and use normal character arrays
 */

/* 
 Notes on software serial:
 Not all pins on the Mega and Mega 2560 support change interrupts,
 so only the following can be used for RX:
 10, 11, 12, 13, 50, 51, 52, 53, 62, 63, 64, 65, 66, 67, 68, 69
 Not all pins on the Leonardo and Micro support change interrupts,
 so only the following can be used for RX:
 8, 9, 10, 11, 14 (MISO), 15 (SCK), 16 (MOSI).
 */
 
#include <SoftwareSerial.h>

const bool debug = false;

unsigned long startTriggerTime = 0;
unsigned long startReadingTime = 0;
unsigned long startTriggerInTime = 0;
unsigned long startOverflowCheckTime = 0;
unsigned long startGoodToGoRequestTime = 0;
int state = 0; // Initialize at state 0
String scannedCode = "";
bool triggerFromPLC = false;
bool overflowDetected = false;
bool good = false;
bool goodToGoRequest = true;

//const unsigned long triggerTimeOut = 5000;
// Changed on 09-02-2018 to try to correct some issues
// (sometimes scanner doesn't read code)
const unsigned long triggerTimeOut = 9000;
const unsigned long readingTimeOut = 500;
const unsigned long triggerInFilter = 5000;
const unsigned long overflowCheckTimeOut = 500;
const unsigned long goodToGoRequestPeriod = 2000;
//const int triggerOutPin = 7;
const int triggerOutPin = 5;
const int goodOrNotPin = 4;
const int triggerInPin = 2;
const int goodToGoPin = 3;
//const int sotwareSerialRXPin = 10;
//const int softwareSerialTXPin = 11;
const int sotwareSerialRXPin = 6;
const int softwareSerialTXPin = 7;
const char triggerChar = 'S';
const char goodToGoSignal = 'G';
const int codeLength = 24;

// RX, TX, enabled inverse logic
SoftwareSerial mySerial(sotwareSerialRXPin, softwareSerialTXPin, 1);

void setup() {
  pinMode(triggerInPin, INPUT);
  pinMode(triggerOutPin, OUTPUT);
  pinMode(goodOrNotPin, INPUT);
  pinMode(goodToGoPin, OUTPUT);
  digitalWrite(goodToGoPin, HIGH); //Relè in logica negativa
  digitalWrite(triggerOutPin, HIGH); //scanner trigger with active-low logic
  // Open serial communications and wait for port to open:
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
  if (debug) Serial.println("Starting!");

  // set the data rate for the SoftwareSerial port
  mySerial.begin(9600);
}

void loop() {
  switch (state) {

    //// STATE 0:
    //// waiting for scan signal from PLC
    case 0:
      // Flushing software serial to filter unwanted noise
      // and possible ssi packets from barcode scanner
      clearSoftwareSerial();
      //clearSerial();
      scannedCode = "";
      good = false;
 
      // Checks if there's a force goodToGo signal
      if (Serial.available()){
        if (Serial.read() == goodToGoSignal){
          //NOTA: relè in logica negativa
          digitalWrite(goodToGoPin, LOW);
          delay(2000);
          digitalWrite(goodToGoPin, HIGH);
        }
      }

      // Check signal from PLC
      if((digitalRead(triggerInPin)) && !triggerFromPLC){
        if (debug) Serial.write("Scanning...\n");
        triggerFromPLC = true;
        digitalWrite(triggerOutPin, LOW);
        startTriggerTimeOut();
        startTriggerInTimeOut(); // starts filter for signal from plc
        state = 1;
        break;
      }

      // Wait for button time to pass (filter on plc signal
      // to avoid multiple readings from the same code)
      if(triggerFromPLC){
        if(isTriggerInTimeOut()){
          triggerFromPLC = false;
        }
      }
      break;

    //// STATE 1:
    //// enabling scanner's trigger and waiting for transmission start
    case 1:
      checkGoodOrNot();
      clearSerial(); // clear hardware serial buffer
      if (isTriggerTimeOut()) {
        raiseError(1);
        digitalWrite(triggerOutPin, HIGH);
        scannedCode = "";
        state = 4;
        break;
      }
      if (mySerial.available()){  // Transmission started
        digitalWrite(triggerOutPin, HIGH);
        startReadingTimeOut();
        state = 2;
        break;
      }
      break;

    //// STATE 2:
    //// Reading data from scanner
    case 2:
      checkGoodOrNot();
      clearSerial(); // clear hardware serial buffer
      if(isReadingTimeOut()){
        raiseError(2);
        scannedCode = "";
        state = 4;
        break;
      }

      // Receive exactly codeLength characters
      if (mySerial.available()){
        scannedCode += (char)mySerial.read();
        if (scannedCode.length() == codeLength){
          startOverflowCheckTimeOut();
          state = 3;
          break;
        }
      }
      break;

    //// State 3:
    //// brief check for overflow (i.e. packet too long)
    case 3:
      checkGoodOrNot();
      if (mySerial.available()){
        scannedCode += (char)mySerial.read();
        overflowDetected = true;
      }
      if(isOverflowCheckTimeOut()){
        if(!overflowDetected){
          if (checkData(scannedCode)){
            // If this point has been reached,
            // everything went allright and the
            // packet, with code and goodOrNot
            // information, can be sent
            sendPacket();
          }
          else{
            raiseError(3);
          }
        }
        else{
          raiseError(4);
          overflowDetected = false;
        }
        scannedCode = "";
        state = 4;
        break;
      }
      break;

    //// STATE 4:
    //// waiting for goodToGo signal from PC
    case 4:
      checkGoodOrNot();
      
      // Sends a periodic goodToGo request signal to PC
      if(goodToGoRequest){
        Serial.println("G");
        startGoodToGoRequestTimeOut();
        goodToGoRequest = false;
      } else if (isGoodToGoRequestTimeOut()){
        goodToGoRequest = true;
      }
      
      if(Serial.available()){
        char signalFromPC = Serial.read();
        if(signalFromPC == goodToGoSignal){
          //NOTA: relè in logica negativa
          digitalWrite(goodToGoPin, LOW);
          delay(2000);
          digitalWrite(goodToGoPin, HIGH);
          state = 0;
          break;
        }
        else if (signalFromPC == triggerChar){
          if (debug) Serial.write("Rescan request from PC. Scanning...\n");
          digitalWrite(triggerOutPin, LOW);
          startTriggerTimeOut();
          scannedCode = "";
          state = 1;
          break;
        }
      }
      break;
  }
}

void checkGoodOrNot(){
  if(digitalRead(goodOrNotPin)) good = true;
}

void sendPacket(){
  Serial.println("D\t" + scannedCode + "\t" + (int)good);
}

// If this function is reached, remember that scannedCode is
// exactly 24 characters long
/*
 * EXPECTED FORMAT:
 *  [00][01][02][03][04][05][06][07][08]  [09]  [10][11][12][13][14][15]    [16]    [17][18]  [19]  [20][21][22][23]
 *  |___________________________________||____||________________________||_______||_________||____||________________|
 *                  0-8:                   9:            10-15:            16:       17-18:   19:        20-23:
 *          readable chars (not accents)  '-'           numbers         'A' or 'B'  numbers   'S'       numbers
 */
bool checkData(String data){
  // TODO check
  int i;
  for(i = 0; i < codeLength; i++){
    char charToCheck = data.charAt(i);
    if (i <= 8){
      if(!(charToCheck >= 32) && (charToCheck <= 126)){
        return false;
      }
    }
    else if(i == 9){
      if(charToCheck != '-'){
        return false;
      }
    }
    else if((i >= 10) && (i <= 15)){
      if(!((charToCheck >= '0') && (charToCheck <= '9'))){
        return false;
      }
    }
    else if(i == 16){
      if(!((charToCheck == 'A') || (charToCheck == 'B'))){
        return false;
      }
    }
    else if((i == 17) || (i == 18)){
      if(!((charToCheck >= '0') && (charToCheck <= '9'))){
        return false;
      }
    }
    else if(i == 19){
      if(charToCheck !=  'S'){
        return false;
      }
    }
    else{
      if(!((charToCheck >= '0') && (charToCheck <= '9'))){
        return false;
      }
    }
  }
  return true;
}

void raiseError(int e){
  switch (e){
    // ERROR 1:
    // Time out, no data at all from scanner
    case 1:
      if (debug) Serial.println("Error 1: timer timed out, no signal from the scanner!");
      Serial.println("E\t1");
      break;

    // ERROR 2:
    // Not enough data
    case 2:
      if (debug) Serial.println("Error 2: not enough data! Received packet: " + scannedCode);
      Serial.println("E\t2");
      break;

    // ERROR 3:
    // Code in bad format
    case 3:
      if (debug) Serial.println("Error 3: code in bad format. Received packet: " + scannedCode);
      Serial.println("E\t3");
      break;

    // ERROR 4:
    // Overflow
    case 4:
      if (debug) Serial.println("Error 4: overflow! Received packet: " + scannedCode);
      Serial.println("E\t4");
      break;
  }
}

void clearSerial(){
  while (Serial.available()) Serial.read();
}

void clearSoftwareSerial(){
  while (mySerial.available()) mySerial.read();
}

bool startTriggerTimeOut() {
  startTriggerTime = millis();
}

bool startGoodToGoRequestTimeOut() {
  startGoodToGoRequestTime = millis();
}

bool startOverflowCheckTimeOut() {
  startOverflowCheckTime = millis();
}

bool startReadingTimeOut() {
  startReadingTime = millis();
}

bool startTriggerInTimeOut() {
  startTriggerInTime = millis();
}

bool isGoodToGoRequestTimeOut(){
  if ((millis() - startGoodToGoRequestTime) > goodToGoRequestPeriod) {
    return true;
  }
  else {
    return false;
  }
}

bool isTriggerTimeOut (){
  if ((millis() - startTriggerTime) > triggerTimeOut) {
    return true;
  }
  else {
    return false;
  }
}

bool isOverflowCheckTimeOut (){
  if ((millis() - startOverflowCheckTime) > overflowCheckTimeOut) {
    return true;
  }
  else {
    return false;
  }
}

bool isReadingTimeOut (){
  if ((millis() - startReadingTime) > readingTimeOut) {
    return true;
  }
  else {
    return false;
  }
}

bool isTriggerInTimeOut (){
  if ((millis() - startTriggerInTime) > triggerInFilter) {
    return true;
  }
  else {
    return false;
  }
}

