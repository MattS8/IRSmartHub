// Copyright 2019 Matthew Steinhardt

#include "ArduinoFirebaseFunctionsESP8266.h"


void ArduinoFirebaseFunctions::connect()
{
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
	Firebase.reconnectWiFi(true);

	if (!Firebase.beginStream(firebaseReadData, ActionPath))
	{
#ifdef AFF_DEBUG
		Serial.println(DEBUG_DIV);
		Serial.println(F("Can't begin stream connection..."));
		Serial.println("REASON: " + firebaseReadData.errorReason());
		Serial.println(DEBUG_DIV);
		Serial.println();
#endif // AFF_DEBUG
	}
}

bool ArduinoFirebaseFunctions::readStreamData()
{
	bool bSuccess = Firebase.readStream(firebaseReadData);

#ifdef AFF_DEBUG
	if (!bSuccess)
	{
		Serial.println(F("Can't read stream data"));
		//Serial.println("REASON: " + firebaseReadData.errorReason());
		Serial.println();
	}
#endif // AFF_DEBUG

	return bSuccess;
}

bool ArduinoFirebaseFunctions::streamTimeout()
{
	bool bSuccess = firebaseReadData.streamTimeout();
#ifdef AFF_DEBUG
#endif //AFF_DEBUG

	return bSuccess;
}




/**
 *	Handles the parsing logic for a HubAction.
**/
void ArduinoFirebaseFunctions::parseJsonToHubAction(const String jsonStr)
{
	//TODO
	const char* startWord = jsonStr.begin();
	const char* endWord = jsonStr.begin();
	int startWordPos = 0;
	int endWordPos = 0;
	String key;
	String strValue;
	uint16_t intValue;

	while (*startWord != '\0' && *endWord != '\0')
	{
		while (*startWord != '\0' && *startWord != '\"') { startWordPos++; startWord++; }
		endWord = ++startWord;
		endWordPos = ++startWordPos;
		while (*endWord != '\0' && *endWord != '\"') { endWordPos++; endWord++; }

		key = jsonStr.substring(startWordPos, endWordPos);

#ifdef AFF_DEBUG
		Serial.print("Key = "); Serial.println(key);
#endif // AFF_DEBUG

		startWord = endWord + 1;
		startWordPos = endWordPos + 1;
		Serial.print("at char: "); Serial.println(*startWord); Serial.println("");

		if (key == "rawData")
		{
			while (*startWord != '\0' && *startWord != '\"') { startWordPos++; startWord++; }
			endWord = ++startWord;
			endWordPos = ++startWordPos;
			while (*endWord != '\0' && *endWord != '\"') { endWordPos++; endWord++; }
			strValue = jsonStr.substring(startWordPos, endWordPos);

#ifdef AFF_DEBUG
			Serial.print("rawData = "); Serial.println(strValue);
#endif // AFF_DEBUG

			hubAction.rawData = String(strValue);
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
		else if (key == "rawLen")
		{
			endWord = ++startWord;
			endWordPos = ++startWordPos;
			while (*endWord != '\0' && *endWord != ',' && *endWord != '}') { endWordPos++; endWord++; }
			intValue = atol(jsonStr.substring(startWordPos, endWordPos).c_str());

#ifdef AFF_DEBUG
			Serial.print("rawLen = "); Serial.println(intValue, DEC);
#endif // AFF_DEBUG

			hubAction.rawLen = intValue;
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
		else if (key == "type")
		{
			endWord = ++startWord;
			endWordPos = ++startWordPos;
			while (*endWord != '\0' && *endWord != ',' && *endWord != '}') { endWordPos++; endWord++; }
			int temp = jsonStr.substring(startWordPos, endWordPos).toInt();

#ifdef AFF_DEBUG
			Serial.print("type = "); Serial.println(temp, DEC);
#endif // AFF_DEBUG

			hubAction.type = temp;
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}

		Serial.println(DEBUG_DIV);
	}

	Serial.println("returning hubAction...");
}

/**
 *	Parses the JSON object received from readStreamData() and
 *	returns a HubAction. If the stream is not available, or 
 *	the read data is not a JSON object, NULL is returned.
 *
 *	NOTE: It is up to the caller to free the allocated
 *	memory when done with the HubAction object.
**/
bool ArduinoFirebaseFunctions::receivedHubAction()
{
	if (firebaseReadData.streamAvailable())
	{
		if (firebaseReadData.dataType() == "json")
		{
#ifdef AFF_DEBUG
			Serial.print("Getting hubAction from: "); Serial.println(firebaseReadData.jsonData());
#endif //AFF_DEBUG
			parseJsonToHubAction(firebaseReadData.jsonData());
			return true;
		}
		else {
#ifdef AFF_DEBUG
			Serial.print(F("firebaseReadData was not a JSON object but of type"));
			Serial.println(firebaseReadData.dataType());
#endif //AFF_DEBUG
		}
	}

	return false;
}
