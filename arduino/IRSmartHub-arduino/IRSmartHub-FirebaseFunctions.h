#ifndef IRSMARTHUB_FIREBASE_FUNCTIONC_H
#define IRSMARTHUB_FIREBASE_FUNCTIONC_H

#include "FirebaseESP8266.h"
#include <ESP8266WiFi.h>

typedef struct HubAction {
	String sender;
	uint16_t* rawData;
	uint16_t rawLen;
	String timestamp;
	bool repeat;
	int type;
} HubAction;

typedef struct HubResult {
	int resultCode;
	String code;
	String timestamp;
	uint16_t encoding;
	String rawData;
	uint16_t rawLen;
	bool repeat;
} HubResult;

static const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
static const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

/* -------------------- 
    Hub Actions
   -------------------- */
static const int IR_ACTION_NONE	 = 0;
static const int IR_ACTION_LEARN = 1;
static const int IR_ACTION_SEND	 = 2;


/* ------------------
    Other Constants
   ------------------ */
static const int DEFAULT_MAX_RETRIES		= 4;
static const uint16_t CHUNK_SIZE			= 50;
static const uint16_t FAILED_DELAY			= 150;
static const uint16_t READ_RAW_DATA_TIMEOUT = 5000;

/* ---- Global Variables ---- */
	HubAction hubAction;
	HubResult hubResult;
	FirebaseData firebaseDataSEND;
	FirebaseData firebaseDataRECV;
	bool newHubActionReceieved = false;							// Used to signal a new action was read


class IRSmartHubFirebaseFunctions {
public:
	// Init Functions
	void connect();

	// Send Functions
	bool sendAction();
	bool sendResult();
	bool sendError(const int errCode);
	bool sendRawData(int index, String rawDataStr);

	// Initialization functions for action/result objects
	void initializeHubAction();
	void initializeHubResult();

	//void setHubName(const String& name);

	//int maxRetries = DEFAULT_MAX_RETRIES;

	//bool readStreamData();
	//bool streamTimeout();

	String ActionPath = "";
	String ResultPath = "";
	String BasePath = "";
	String SetupPath = "";

private:
	bool sendToFirebase(const String& path, FirebaseJson& firebaseJson);
	

	//FirebaseJson parseHubResultToJson();
	//void parseJsonToHubAction(const String jsonStr);
	//void getNextWord(const char* &startWord, const char* &endWord, int& startWordPos, int& endWordPos);
	//void getNextNumber(const char* &startWord, const char*& endWord, int& startWordPos, int& endWordPos);


};

#endif