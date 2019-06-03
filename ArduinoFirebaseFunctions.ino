//Copyright 2019 Matthew Steinhardt

#include "ArduinoFirebaseFunctions.h"

/**
 *
 **/
void ArduinoFirebaseFunctions::connect()
{
	if (bDEBUG) { Serial.print("Connecting to firebase at: "); Serial.println(ActionPath.c_str()); }

	// Initialize Firebase
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
	
	// Initialize action root for device
	sprintf(responseBuffer, "{\"type\": %d, \"timestamp\": \"%lu\"}", 
		IR_ACTION_NONE,
		millis());
	FirebaseObject obj = FirebaseObject(responseBuffer);
	Firebase.set(FirebaseFunctions.ActionPath, obj.getJsonVariant());

	if (bDEBUG) Serial.println("Done sending initialization post.");

	// Begin listening for actions
	Firebase.stream(ActionPath);
}

/**
 *
 **/
void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	Serial.println("TODO - setHubName");
}

/**
 *
 **/
void ArduinoFirebaseFunctions::sendRecordedSignal(decode_results* results)
{
	Serial.println("TODO - sendRecordedSignal");
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

	if (Firebase.failed()) 
		Serial.print("Failed to send error response... ");	
	else if (bDEBUG) 
		Serial.println("Sent error result");
}

/**
 *
 **/
void ArduinoFirebaseFunctions::setDebug(bool debug)
{
	bDEBUG = debug;
}