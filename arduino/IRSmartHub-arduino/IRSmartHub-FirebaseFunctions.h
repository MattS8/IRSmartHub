#ifndef IRSMARTHUB_FIREBASE_FUNCTIONC_H
#define IRSMARTHUB_FIREBASE_FUNCTIONC_H

#include "FirebaseESP8266.h"
#include <ESP8266WiFi.h>
#include "IRrecv.h"						// Used to send IR data from decode_results

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
    Result Codes
    -------------------- */
static const int RES_SEND_SIG	= 700;
static const int RES_SEND_SUCC	= 701;
static const int ERR_UNKNOWN	= 800;
static const int ERR_TIMEOUT	= 801;
static const int ERR_OVERFLOW	= 802;

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


class IRSmartHubFirebaseFunctions {
public:
	void connect();
	bool sendAction();
	bool sendResult();
	//void setHubName(const String& name);

	//void sendError(const int errCode);
	//void sendRecordedSignal(const decode_results& results);

	//int maxRetries = DEFAULT_MAX_RETRIES;

	//bool readStreamData();
	//bool streamTimeout();

	String ActionPath = "";
	String ResultPath = "";
	String BasePath = "";
	String SetupPath = "";


#ifdef IRSMARTHUB_UNIT_TESTS
	int test_parseHubResultToJson();
	int test_parseJsonToHubAction();
	int test_parseHubActionToJson();
#endif

private:
	bool sendToFirebase(const String& path, FirebaseJson& firebaseJson);

	//void sendRawData(const decode_results& results);

	//void readRawData(uint16_t numChunks);

	//String resultToHexidecimal(const decode_results& result);
	//uint16_t getCorrectedRawLength(const decode_results& results);

	/*String	rawDataToString(const decode_results& results, uint16_t startPos);*/

	//FirebaseJson parseHubResultToJson();
	//void parseJsonToHubAction(const String jsonStr);
	//void getNextWord(const char* &startWord, const char* &endWord, int& startWordPos, int& endWordPos);
	//void getNextNumber(const char* &startWord, const char*& endWord, int& startWordPos, int& endWordPos);

	void initializeHubAction();
	void initializeHubResult();
};

#endif