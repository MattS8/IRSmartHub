#ifndef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
#define ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H


#include <FirebaseArduino.h>
#include <ESP8266WiFi.h>
#include "IRrecv.h"						// Used to send IR data from decode_results

#ifdef AFF_DEBUG
#include "IRSmartHubDebug.h"
#endif // AFF_DEBUG


typedef struct HubAction {
	String sender;
	String rawData;
	uint16_t rawLen;
	String timestamp;
	bool repeat;
	int type;
} HubAction;

typedef struct HubResult {
	int resultCode;
	String code;
	String timestamp;
	String encoding;
	String rawData;
	uint16_t rawLen;
	bool repeat;
} HubResult;

static const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
static const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";

/* Database Object JSON Strings */

/* --------- HubResult/HubAction Const Strings ---------- */
static const String HR_STR_RES_CODE		= "{\"resultCode\": ";
static const String HR_STR_CODE			= ", \"code\": \"";
static const String HR_STR_SENDER		= "\"sender\": \"";
static const String HR_STR_TIMESTAMP	= "\", \"timestamp\": \"";
static const String HR_STR_TYPE			= "\", \"type\": ";
static const String HR_STR_RAW_DATA		= ", \"rawData\": \"";
static const String HR_STR_ENCODING		= "\", \"encoding\": \"";
static const String HR_STR_RAW_LEN		= "\", \"rawLen\": ";
static const String HR_STR_DATA_CHUNKS	= ", \"numDataChunks\": ";
static const String HR_STR_REPEAT		= ", \"repeat\": ";

/* -------------------- Result Codes -------------------- */
static const int RES_SEND_SIG	= 700;
static const int ERR_UNKNOWN	= 800;
static const int ERR_TIMEOUT	= 801;
static const int ERR_OVERFLOW	= 802;

/* -------------------- Hub Actions -------------------- */
static const int IR_ACTION_NONE	 = 0;
static const int IR_ACTION_LEARN = 1;
static const int IR_ACTION_SEND	 = 2;

static const int DEFAULT_MAX_RETRIES = 4;
static const int CHUNK_SIZE = 50;
static const uint16_t FAILED_DELAY = 150;

HubAction hubAction;
HubResult hubResult;

class ArduinoFirebaseFunctions {
public:
	void setup();
	void connect();
	void setHubName(const String& name);

	void sendError(const int errCode);
	void sendRecordedSignal(const decode_results& results);

	int maxRetries = DEFAULT_MAX_RETRIES;

	bool readStreamData();
	bool streamTimeout();

	bool receivedHubAction();

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
	void sendToFirebase(const String& path, const JsonVariant& obj);
	void sendStringToFirebase(const String& path, const String& message);
	void sendRawData(const decode_results& results);

	String resultToHexidecimal(const decode_results& result);
	uint16_t getCorrectedRawLength(const decode_results& results);
	String uint64ToString(uint64_t input, uint8_t base = 10);
	String rawDataToString(const decode_results& results, uint16_t startPos);

	String parseHubResultToJson();
	String parseHubActionToJson();
	void parseJsonToHubAction(const String jsonStr);
	void getNextWord(const char* &startWord, const char* &endWord, int& startWordPos, int& endWordPos);
	void getNextNumber(const char* &startWord, const char*& endWord, int& startWordPos, int& endWordPos);

	void initializeHubAction();
	void initializeHubResult();
};

	static uint16_t getCorrectedChunkCount(uint16_t rawLen);

#endif