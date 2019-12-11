 /*  
  *  GSM Tracking System
  *  Code for Arduinos that'll be placed next to test machines,
  *  connected directly with barcode scanners.
  *  One Arduino for each machine, one scanner each.
  *  Pc will communicate with scanner and machine ONLY
  *  through arduino usb cable.
  *  
  *  State description (retained by the variable 'state'):
  *  STATE 0: waiting for signal from PC or PLC and clearing
  *           software serial buffer.
  *  STATE 1: enabling scanner's trigger, waiting for transmission
  *           start from scanner and clearing hardware serial buffer.
  *  STATE 2: receiving data from scanner.
  */

////////// TO DO:
/*
 *    - implement check on code
 *    - implement good or no good input
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

const bool debug = true;

unsigned long startTriggerTime = 0;
unsigned long startReadingTime = 0;
unsigned long startTriggerInTime = 0;
unsigned long startOverflowCheckTime = 0;
int state = 0; // Initialize at state 0
String scannedCode;
bool triggerFromPLC = false;
bool overflowDetected = false;

const unsigned long triggerTimeOut = 5000;
const unsigned long readingTimeOut = 500;
const unsigned long triggerInFilter = 5000;
const unsigned long overflowCheckTimeOut = 500;
const int triggerOutPin = 7;
const int goodNoGoodPin = 4;
const int triggerInPin = 2;
const int sotwareSerialRXPin = 10;
const int softwareSerialTXPin = 11;
const char triggerChar = 'S';
const int codeLength = 24;
const int checkCode = true;

// RX, TX, enabled inverse logic
SoftwareSerial mySerial(sotwareSerialRXPin, softwareSerialTXPin, 1);

void setup() {
  pinMode(triggerInPin, INPUT);
  pinMode(triggerOutPin, OUTPUT);
  pinMode(goodNoGoodPin, OUTPUT);
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

    //// STATE 0: waiting for data from pc
    case 0:
      // Flushing software serial to filter unwanted noise
      // and ssi packets from barcode scanner
      clearSoftwareSerial();
      scannedCode = "";

      // Check signal from PC
      if (Serial.available()){
        if (Serial.read() == triggerChar){
          if (debug) Serial.write("Scanning...\n");
          digitalWrite(triggerOutPin, LOW);
          startTriggerTimeOut();
          state = 1;
          break;
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

    //// STATE 1: enabling scanner's trigger and waiting for transmission start
    case 1:
      clearSerial(); // clear hardware serial buffer
      if (isTriggerTimeOut()) {
        raiseError(1);
        digitalWrite(triggerOutPin, HIGH);
        state = 0;
        break;
      }
      if (mySerial.available()){  // Transmission started
        digitalWrite(triggerOutPin, HIGH);
        startReadingTimeOut();
        state = 2;
        break;
      }
      break;

    //// STATE 2: Reading data from scanner
    case 2:
      clearSerial(); // clear hardware serial buffer
      if(isReadingTimeOut()){
        raiseError(2);
        state = 0;
        break;
      }
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
      if (mySerial.available()){
        scannedCode += (char)mySerial.read();
        overflowDetected = true;
      }
      if(isOverflowCheckTimeOut()){
        if(!overflowDetected){
          if (checkData(scannedCode)){
            Serial.println(scannedCode);
          }
          else{
            raiseError(3);
          }
          break;
        }
        else{
          raiseError(4);
          overflowDetected = false;
          break;
        }
        state = 0;
      }
      break;
  }
}

bool checkData(String data){
  // TODO check
  return true;
}

void raiseError(int e){
  switch (e){
    // ERROR 1:
    // time out, no data at all from scanner
    case 1:
      if (debug) Serial.println("Error 1: timer timed out, no signal from the scanner!");
      break;

    // ERROR 2:
    // Not enough data
    case 2:
      if (debug) Serial.println("Error 2: not enough data! Received packet: " + scannedCode);
      break;

    // ERROR 3:
    // Code in bad format (overflow could have happened too)
    case 3:
      Serial.println("Error 3: code in bad format.");
      break;

    case 4:
      Serial.println("Error 4: overflow! Received packet: " + scannedCode);
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

bool startOverflowCheckTimeOut() {
  startOverflowCheckTime = millis();
}

bool startReadingTimeOut() {
  startReadingTime = millis();
}

bool startTriggerInTimeOut() {
  startTriggerInTime = millis();
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

