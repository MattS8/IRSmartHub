#ifndef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
#define ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H


#include <FirebaseESP8266.h>
#include <ESP8266WiFi.h>          		// ESP8266 Core WiFi Library
#include "IRrecv.h"						// Used to send IR data from decode_results

#define AFF_DEBUG 1

typedef struct HubAction {
	String sender;
	String rawData;
	uint16_t rawLen;
	String timestamp;
	int type;
} HubAction;

typedef struct HubResult {
	int resultCode;
	unsigned long timestamp;
	String encoding;
	String code;
	String rawData;
	uint16_t rawLen;
} HubResult;

const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

/* Database Object JSON Strings */
const char JSON_Recorded_Signal[] = "{\"resultCode\": %d, \"encoding\": \"%s\", \"code\": \"0x%s\", \"timestamp\": \"%lu\", \"rawData\": \"%s\", \"rawLen\": %lu}";

const size_t MAX_RESPONSE_SIZE = 1024;

/* -------------------- Result Codes -------------------- */
const int RES_SEND_SIG	= 700;
const int ERR_UNKNOWN	= 800;
const int ERR_TIMEOUT	= 801;
const int ERR_OVERFLOW	= 802;

/* -------------------- Hub Actions -------------------- */
const int IR_ACTION_NONE = 0;
const int IR_ACTION_LEARN = 1;
const int IR_ACTION_SEND = 2;

FirebaseData firebaseReadData;
//FirebaseData firebaseWriteData;

HubAction hubAction;
HubResult hubResult;

class ArduinoFirebaseFunctions {
public:
	void connect();
	bool readStreamData();
	bool streamTimeout();

	bool receivedHubAction();

	String ActionPath = "";
	String ResultPath = "";
	String BasePath = "";
	String SetupPath = "";

private:
	void parseJsonToHubAction(const String jsonStr);
};

#endif