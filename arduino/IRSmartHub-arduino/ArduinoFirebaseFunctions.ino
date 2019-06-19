////Copyright 2019 Matthew Steinhardt
//
//#include "ArduinoFirebaseFunctions.h"
//
///** 
// *	Convert the result's value/state to simple hexadecimal. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266) 
//**/
//String ArduinoFirebaseFunctions::resultToHexidecimal(const decode_results* result)
//{
//	String output = "";
//	if (hasACState(result->decode_type)) 
//	{
//#if DECODE_AC
//		for (uint16_t i = 0; result->bits > i * 8; i++) {
//			if (result->state[i] < 0x10) output += '0';  // Zero pad
//			output += uint64ToString(result->state[i], 16);
//		}
//#endif
//	}
//	else 
//	{
//		output += uint64ToString(result->value, 16);
//	}
//
//	return output;
//}
//
///**
// *	Return the corrected length of a 'raw' format array structure after over-large values are
// *	converted into multiple entries. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
//**/
//uint16_t ArduinoFirebaseFunctions::getCorrectedRawLength(const decode_results* results) 
//{
//	uint16_t extended_length = results->rawlen - 1;
//	for (uint16_t i = 0; i < results->rawlen - 1; i++) 
//	{
//		uint32_t usecs = results->rawbuf[i] * kRawTick;
//		// Add two extra entries for multiple larger than UINT16_MAX it is.
//		extended_length += (usecs / (UINT16_MAX + 1)) * 2;
//	}
//
//	return extended_length;
//}
//
///**
// *	Converts a uint64_t to a string. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
//**/
//String ArduinoFirebaseFunctions::uint64ToString(uint64_t input, uint8_t base)
//{
//	String result = "";
//	// Check we have a base that we can actually print.
//	// i.e. [0-9A-Z] == 36
//	if (base < 2 || base > 36) base = 10;
//
//	do 
//	{
//		char c = input % base;
//		input /= base;
//
//		c += c < 10 ? '0' : 'A' - 10;
//
//		result = c + result;
//	} while (input);
//
//	return result;
//}
//
///**
// *	Converts the raw data from results to a string. Used
// *  to store in Firebase. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
// **/
//String ArduinoFirebaseFunctions::rawDataToString(const decode_results* results)
//{
//	String output = "";
//	// Dump data
//	for (uint16_t i = 1; i < results->rawlen; i++) 
//	{
//		uint32_t usecs;
//		for (usecs = results->rawbuf[i] * kRawTick; usecs > UINT16_MAX; usecs -= UINT16_MAX) 
//		{
//			output += uint64ToString(UINT16_MAX);
//			if (i % 2)
//				output += F(", 0,  ");
//			else
//			output += F(",  0, ");
//		}
//		output += uint64ToString(usecs, 10);
//		if (i < results->rawlen - 1)
//			output += F(", ");            // ',' not needed on the last one
//		if (i % 2 == 0) 
//			output += ' ';  // Extra if it was even.
//	}
//
//	// End declaration
//	output += F("\0");
//
//	return output;
//}
//
///**
// *
// **/
//void ArduinoFirebaseFunctions::connect()
//{
//	 if (bConnected) {
//	 	if (bDEBUG) Serial.println("Already connected!");
//	 	return;
//	 }
//	if (bDEBUG) { Serial.print("Connecting to firebase at: "); Serial.println(SetupPath.c_str()); }
//
//	// Initialize Firebase
//	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
//
//	if (Firebase.failed()) {
//		Serial.println("Failed to begin...");
//		ESP.reset();
//	}
//	
//	// Initialize action root for device
//	sprintf(responseBuffer, "{\"type\": %d, \"timestamp\": \"%lu\", \"sender\": \" \"}", 
//		IR_ACTION_NONE,
//		millis());
//	FirebaseObject obj = FirebaseObject(responseBuffer);
//	Firebase.set(ActionPath, obj.getJsonVariant("/"));
//	delay(500);
//
//	if (Firebase.failed()) {
//		Serial.println("Failed to send intial action response...");
//		ESP.reset();
//	}
//
//	// Send initial setup notification to firebase w/username
//	if (FirebaseFunctions.SetupPath != "") 
//		Firebase.setBool(FirebaseFunctions.SetupPath, 1);
//
//	if (Firebase.failed()) {
//		Serial.println("Failed to send intialization response...");
//		ESP.reset();
//	}
//
//	delay(500);
//	
//
//	if (bDEBUG) Serial.println("Done sending initialization post.");
//
//	// NOTE: Something causes the esp to crash after initial connection
//	//  The current solution is to just let it  crash and restart after 
//	//  first successful connection. Further research into this matter 
//	//  is needed.
//	
//	// Begin listening for actions
//	Firebase.stream(ActionPath);
//	delay(1000);
//
//	bConnected = true;
//}
//
///**
// *
// **/
//void ArduinoFirebaseFunctions::setHubName(const String& name)
//{
//	sprintf(responseBuffer, "%s/name", BasePath.c_str());
//	String namePath = String(responseBuffer);
//	Firebase.setString(namePath, name);
//
//	if (Firebase.failed())
//	{
//		if (bDEBUG) Serial.println("Failed to set hub name.");
//		sendError(ERR_UNKNOWN);
//		ESP.reset();
//	}
//	else if (bDEBUG) { Serial.print("Changed hub name to "); Serial.println(name); }
//}
//
///**
// *
// **/
//void ArduinoFirebaseFunctions::sendRecordedSignal(const decode_results* results)
//{
//	String output = rawDataToString(results);
//	String jsonObjectStr = "{\"resultCode\": ";
//		sprintf(responseBuffer, "%d", RES_SEND_SIG);
//		jsonObjectStr += responseBuffer;
//		jsonObjectStr += ", \"encoding\": \"" + typeToString(results->decode_type, results->repeat);
//		jsonObjectStr += "\", \"code\": \"0x" + resultToHexidecimal(results);
//		jsonObjectStr += "\", \"timestamp\": \"";
//		sprintf(responseBuffer, "%lu", millis());
//		jsonObjectStr += responseBuffer;
//		jsonObjectStr += "\", \"rawData\": \"" + output;
//		jsonObjectStr += "\", \"rawLen\": ";
//		sprintf(responseBuffer, "%lu", getCorrectedRawLength(results));
//		jsonObjectStr += responseBuffer;
//		jsonObjectStr += "}";
//
//
//	//sprintf(responseBuffer, JSON_Recorded_Signal, 
//	//	RES_SEND_SIG, /* resultCode */
//	//	typeToString(results->decode_type, results->repeat), /* encoding */
//	//	resultToHexidecimal(results), /* code */
//	//	millis(), /* timestamp */
//	//	output.c_str(), /* rawData */
//	//	results->rawlen-1); /* rawLen */
//
//	if (bDEBUG) { Serial.print("Sending: "); Serial.println(jsonObjectStr); }
//	FirebaseObject obj = FirebaseObject(jsonObjectStr.c_str());
//	Firebase.set(FirebaseFunctions.ResultPath, obj.getJsonVariant("/"));
//
//	if (Firebase.failed()) 
//	{
//		if (bDEBUG) Serial.println("Failed to send signal result...");
//		ESP.reset();
//		sendError(ERR_UNKNOWN);
//	}
//	else if (bDEBUG) Serial.println("Sent signal result.");
//}
//
///**
// *
// **/
//void ArduinoFirebaseFunctions::sendError(const int errorType)
//{
//	sprintf(responseBuffer, "{\"code\": %d, \"timestamp\": \"%lu\"}",
//		errorType,
//		millis());
//	FirebaseObject obj = FirebaseObject(responseBuffer);
//	Firebase.set(FirebaseFunctions.ResultPath, obj.getJsonVariant());
//
//	if (bDEBUG) {
//		if (Firebase.failed()) {
//			Serial.print("Failed to send error response... ");	
//			ESP.reset();
//		}
//		else
//			Serial.println("Sent error result.");
//	}
//}
//
///**
// *
// **/
//void ArduinoFirebaseFunctions::setDebug(bool debug)
//{
//	bDEBUG = debug;
//}