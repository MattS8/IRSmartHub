//Copyright 2019 Matthew Steinhardt

#ifndef ARDUINO_FIREBASE_FUNCTIONS_H
#define ARDUINO_FIREBASE_FUNCTIONS_H

#include <FirebaseArduino.h>			// https://github.com/FirebaseExtended/firebase-arduino
#include <ArduinoJson.h>				// https://github.com/bblanchon/ArduinoJson
#include <IRrecv.h>						// Used to send IR data from decode_results


const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

const size_t MAX_RESPONSE_SIZE = 1024;

/* -------------------- Result Codes -------------------- */
const int RES_SEND_SIG = 700;
const int ERR_UNKNOWN  = 800;
const int ERR_TIMEOUT  = 801;
const int ERR_OVERFLOW = 802;

class ArduinoFirebaseFunctions {
public:
	void setHubName(const String& name);

	void connect();

	void sendRecordedSignal(decode_results* results);

	void sendError(const int errorType);

	void setDebug(bool debug);

	String ActionPath;
	String ResultPath;
	String BasePath;

private:
	char* responseBuffer = new char[MAX_RESPONSE_SIZE];
	bool bDEBUG = false;
};

#endif