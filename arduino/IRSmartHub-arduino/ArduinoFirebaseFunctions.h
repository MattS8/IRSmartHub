////Copyright 2019 Matthew Steinhardt
//
//#ifndef ARDUINO_FIREBASE_FUNCTIONS_H
//#define ARDUINO_FIREBASE_FUNCTIONS_H
//
//#include "FirebaseArduino.h"			// https://github.com/FirebaseExtended/firebase-arduino
//#include "ArduinoJson.h"				// https://github.com/bblanchon/ArduinoJson
//#include "IRrecv.h"						// Used to send IR data from decode_results
//
//
//const String FIREBASE_HOST = "ir-home-hub.firebaseio.com";
//const String FIREBASE_AUTH = "OVupEOIVjxTW1brlm02WISnExnOWRBxc9yhJVyPy";
//
///* Database Object JSON Strings */
//const char JSON_Recorded_Signal[] = "{\"resultCode\": %d, \"encoding\": \"%s\", \"code\": \"0x%s\", \"timestamp\": \"%lu\", \"rawData\": \"%s\", \"rawLen\": %lu}";
//
//const size_t MAX_RESPONSE_SIZE = 1024;
//
///* -------------------- Result Codes -------------------- */
//const int RES_SEND_SIG = 700;
//const int ERR_UNKNOWN  = 800;
//const int ERR_TIMEOUT  = 801;
//const int ERR_OVERFLOW = 802;
//
//class ArduinoFirebaseFunctions {
//public:
//	void setHubName(const String& name);
//
//	void connect();
//
//	void sendRecordedSignal(const decode_results* results);
//
//	void sendError(const int errorType);
//
//	void setDebug(bool debug);
//
//	String ActionPath = "";
//	String ResultPath = "";
//	String BasePath = "";
//	String SetupPath = "";
//
//private:
//	String rawDataToString(const decode_results* results);
//	String uint64ToString(uint64_t input, uint8_t base = 10);
//	String resultToHexidecimal(const decode_results* result);
//	uint16_t getCorrectedRawLength(const decode_results* results);
//
//	char* responseBuffer = new char[MAX_RESPONSE_SIZE];
//	bool bDEBUG = false;
//	bool bConnected = false;
//};
//
//#endif