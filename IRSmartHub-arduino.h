#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#include <FirebaseArduino.h>			//https://github.com/FirebaseExtended/firebase-arduino
#include <ESP8266WiFi.h>          		//ESP8266 Core WiFi Library
#include <ArduinoJson.h>				//https://github.com/bblanchon/ArduinoJson
#include <DNSServer.h>            		//Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     		//Local WebServer used to serve the configuration portal
#include <WiFiManager.h>          		//https://github.com/tzapu/WiFiManager WiFi Configuration Magic

#include <IRsend.h>
#include <IRrecv.h>
#include <IRremoteESP8266.h>
#include <IRutils.h>


#define IR_RECV_PIN 14
#define IR_BLAST_PIN 100

const uint32_t IR_READ_TIMEOUT = 10000;
const uint32_t SMART_HUB_BAUD_RATE = 115200;
const uint8_t  IR_RECV_MESSAGE_TIMEOUT = 50;
const uint16_t IR_RECV_BUFFER_SIZE = 1024;

const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

/* -------------------- Error Codes -------------------- */
const int ERR_TIMEOUT = 800;
const int ERR_OVERFLOW = 801;

/* -------------------- Hub Actions -------------------- */
const int IR_ACTION_NONE = 0;
const int IR_ACTION_LEARN = 1;
const int IR_ACTION_SEND = 2;

class ArduinoIRFunctions {
public:
	/**
	 * Listens on IR receiver for the next valid IR signal and
	 * returns the IR code as a string.
	**/
	void readNextSignal();

	void sendSignal(const String& irSignal, bool bRepeat);

private:
	IRrecv irReceiver = IRrecv(IR_RECV_PIN, 
							   IR_RECV_BUFFER_SIZE, 
							   IR_RECV_MESSAGE_TIMEOUT, 
							   true);
	IRsend irSender = IRsend(IR_BLAST_PIN);
};

class ArduinoFirebaseFunctions {
public:
	void setHubName(const String& name);

	void connect();

	void sendRecordedSignal(decode_results* results);

	void sendError(const int errorType);

private:
	String IR_UID = "JFIERKKFCIIKERI";
};

#endif