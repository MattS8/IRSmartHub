//Copyright 2019 Matthew Steinhardt

#include "ArduinoFirebaseFunctions.h"

/**
 *	Converts the raw data from results to a string. Used
 *  to store in Firebase. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
 **/
String ArduinoFirebaseFunctions::rawDataToString(decode_results* results)
{
	String output = "{";

	// Dump data
	for (uint16_t i = 1; i < results->rawlen; i++) 
	{
		uint32_t usecs;
		for (usecs = results->rawbuf[i] * kRawTick; usecs > UINT16_MAX; usecs -= UINT16_MAX) 
		{
			output += uint64ToString(UINT16_MAX);
			if (i % 2)
				output += F(", 0,  ");
			else
			output += F(",  0, ");
		}
		output += uint64ToString(usecs, 10);
		if (i < results->rawlen - 1)
			output += F(", ");            // ',' not needed on the last one
		if (i % 2 == 0) 
			output += ' ';  // Extra if it was even.
	}

	// End declaration
	output += F("};");

	return output;
}

/**
 *
 **/
void ArduinoFirebaseFunctions::connect()
{
	if (bConnected) {
		if (bDEBUG) Serial.println("Already connected!");
		return;
	}

	if (bDEBUG) { Serial.print("Connecting to firebase at: "); Serial.println(ActionPath.c_str()); }

	// Initialize Firebase
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
	
	// Initialize action root for device
	sprintf(responseBuffer, "{\"type\": %d, \"timestamp\": \"%lu\"}", 
		IR_ACTION_NONE,
		millis());
	FirebaseObject obj = FirebaseObject(responseBuffer);
	Firebase.set(ActionPath, obj.getJsonVariant("/"));

	// Send initial setup notification to firebase w/username
	if (FirebaseFunctions.SetupPath != "") 
		Firebase.setBool(FirebaseFunctions.SetupPath, 1);
	

	if (bDEBUG) Serial.println("Done sending initialization post.");

	// NOTE: Something causes the esp to crash after initial connection
	//  The current solution is to just let it  crash and restart after 
	//  first successful connection. Further research into this matter 
	//  is needed.
	
	// Begin listening for actions
	Firebase.stream(ActionPath);

	bConnected = true;
}

/**
 *
 **/
void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	sprintf(responseBuffer, "%s/name", BasePath.c_str());
	String namePath = String(responseBuffer);
	Firebase.setString(namePath, name);

	if (Firebase.failed())
	{
		if (bDEBUG) Serial.println("Failed to set hub name.");
		sendError(ERR_UNKNOWN);
	}
	else if (bDEBUG) { Serial.print("Changed hub name to "); Serial.println(name); }
}

/**
 *
 **/
void ArduinoFirebaseFunctions::sendRecordedSignal(decode_results* results)
{
	sprintf(responseBuffer, "{\"code\": %d, \"timestamp\": \"%lu\", "
		"\"rawData\": \"%s\", \"rawLen\": %lu}",
		RES_SEND_SIG, millis(), rawDataToString(results).c_str(), results->rawlen);
	if (bDEBUG) { Serial.print("Sending: "); Serial.println(responseBuffer); }
	FirebaseObject obj = FirebaseObject(responseBuffer);
	Firebase.set(FirebaseFunctions.ResultPath, obj.getJsonVariant("/"));

	if (Firebase.failed()) 
	{
		if (bDEBUG) Serial.println("Failed to send signal result...");
		sendError(ERR_UNKNOWN);
	}
	else if (bDEBUG) Serial.println("Sent signal result.");
}

/**
 *
 **/
void ArduinoFirebaseFunctions::sendError(const int errorType)
{
	sprintf(responseBuffer, "{\"code\": %d, \"timestamp\": \"%lu\"}",
		errorType,
		millis());
	FirebaseObject obj = FirebaseObject(responseBuffer);
	Firebase.set(FirebaseFunctions.ResultPath, obj.getJsonVariant());

	if (bDEBUG) {
		if (Firebase.failed()) 
			Serial.print("Failed to send error response... ");	
		else
			Serial.println("Sent error result.");
	}
}

/**
 *
 **/
void ArduinoFirebaseFunctions::setDebug(bool debug)
{
	bDEBUG = debug;
}