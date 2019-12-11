 /*  
  *  GSM Tracking System
  *  Code for Arduinos that'll be placed nexto to test machines, connected
  *  directly with barcode scanners.
  *  One Arduino for each machine, one scanner each.
  *  Pc will communicate with scanner and machine ONLY through arduino usb cable.
  *  
  *  State description (retained by the variable 'state'):
  *  STATE 0: waiting for signal from PC or PLC and clearing software serial buffer.
  *  STATE 1: enabling scanner's trigger, waiting for transmission start from scanner
  *           and clearing hardware serial buffer.
  *  STATE 2: receiving data from scanner.
  */

////////// TO DO:
/*
 *    - implement check on code
 *    - implement button (trigger from plc)
 *    - then you'll be done!
 */

/* 
 NOTES ON SOFTWARE SERIAL
 The circuit:
 * RX is digital pin 10 (connect to TX of other device)
 * TX is digital pin 11 (connect to RX of other device)
 Note:
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
int state = 0; // Initialize at state 0
String scannedCode;
bool triggerFromPLC = false;

const unsigned long triggerTimeOut = 5000;
const unsigned long readingTimeOut = 500;
const unsigned long triggerInFilter = 5000;
const int triggerOutPin = 7;
const int goodNoGoodPin = 4;
const int triggerInPin = 2;
const int sotwareSerialRXPin = 10;
const int softwareSerialTXPin = 11;
const char triggerChar = 'S';
const int codeLength = 24;

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

    // STATE 0: waiting for data from pc
    case 0:
      // Flushing software serial to filter unwanted noise
      // and ssi packets from barcode scanner
      clearSoftwareSerial();
      scannedCode = "";

      // Signal from PC
      if (Serial.available()){
        if (Serial.read() == triggerChar){
          if (debug) Serial.write("Scanning...\n");
          digitalWrite(triggerOutPin, LOW);
          startTriggerTimeOut();
          state = 1;
        }
      }

      // Signal from PLC
      if((digitalRead(triggerInPin)) && (state == 0) && !triggerFromPLC){
        if (debug) Serial.write("Scanning...\n");
        triggerFromPLC = true;
        digitalWrite(triggerOutPin, LOW);
        startTriggerTimeOut();
        startTriggerInTimeOut(); // starts filter for signal from plc
        state = 1;
      }

      if(triggerFromPLC && (state == 0)){
        if(isTriggerInTimeOut()){
          triggerFromPLC = false;
        }
      }
      break;

    // STATE 1: enabling scanner's trigger and waiting for transmission start
    case 1:
      clearSerial(); // clear hardware serial buffer
      if (isTriggerTimeOut()) {
        if (debug) Serial.write("Error: timer timed out, no signal from the scanner!\n");
        digitalWrite(triggerOutPin, HIGH);
        state = 0;
      }
      if (mySerial.available() && (state == 1)){  // Transmission started
        digitalWrite(triggerOutPin, HIGH);
        startReadingTimeOut();
        state = 2;
      }
      break;

    // STATE 2: Reading data from scanner
    case 2:
      clearSerial(); // clear hardware serial buffer
      if(isReadingTimeOut()){
        if (debug) Serial.println("Error: not enough data! Received packet:" + scannedCode);
        state = 0;
      }
      if (mySerial.available() && (state == 2)){
        scannedCode += (char)mySerial.read();
        if (scannedCode.length() == codeLength){
          if (checkData(scannedCode)){
            Serial.println(scannedCode);
          }
          else{
            Serial.println("Error: code in bad format.");
          }
          state = 0;
        }
      }
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

bool checkData(String data){
  // TODO check
  return true;
}

