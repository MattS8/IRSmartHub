#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#include <FirebaseArduino.h>			//https://github.com/FirebaseExtended/firebase-arduino
#include <ESP8266WiFi.h>          		//ESP8266 Core WiFi Library
#include <ArduinoJson.h>				//https://github.com/bblanchon/ArduinoJson

#include <DNSServer.h>            		//Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     		//Local WebServer used to serve the configuration portal
#include <WiFiManager.h>          		//https://github.com/tzapu/WiFiManager WiFi Configuration Magic
#include "IRLibAll.h"					//https://github.com/cyborg5/IRLib2

#define IR_RECV_PIN 6

const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

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

	void sendSignal(const String& irSignal);

	void setup();

private:
	String lastReadSignal;
	IRrecvPCI irReceiver(IR_RECV_PIN);
	IRDecode irDecoder;
	// TODO IRrecv irrecv(IR_RECV_PIN);
};

class ArduinoFirebaseFunctions {
public:
	void setHubName(const String& name);

	void connect();

private:
	String IR_UID = "JFIERKKFCIIKERI";
};

#endif