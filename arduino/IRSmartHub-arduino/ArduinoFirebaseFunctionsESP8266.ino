#include "ArduinoFirebaseFunctionsESP8266.h"
// Copyright 2019 Matthew Steinhardt
#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
/** Initializes global data objects. **/
void ArduinoFirebaseFunctions::setup()
{
	initializeHubAction();
	initializeHubResult();
}

/**	Connects to firebase endpoint and begins streaming. **/
void ArduinoFirebaseFunctions::connect()
{
	// Start firebase connection
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);

	// Extra safe initialization of HubAction
	initializeHubAction();

	// Set initial action to NONE
	FirebaseObject obj = FirebaseObject(parseHubActionToJson().c_str());
	sendToFirebase(ActionPath, obj.getJsonVariant("/"));
	delay(300);

	if (Firebase.failed())
	{
#ifdef AFF_DEBUG
		Serial.println(F("Failed to set initial action..."));
#endif // AFF_DEBUG
		ESP.reset();
	}

	Firebase.stream(ActionPath);
#ifdef AFF_DEBUG
	Serial.println("Started streaming...");
#endif // AFF_DEBUG

}

/**
 *	Sets the name of the hub in firebase.
 **/
void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	Firebase.setString(String(BasePath + "/name").c_str(), name.c_str());

#ifdef AFF_DEBUG
	if (Firebase.failed())
	{
		Serial.println("Failed to set name...");
	}
#endif // AFF_DEBUG

}

/**
 *	Determines if new data has been received from
 *	Firebase. If so, the data is parsed into hubAction
 *	and returns true. Otherwise, false is returned.
**/
bool ArduinoFirebaseFunctions::receivedHubAction()
{
	if (Firebase.available())
	{
#ifdef AFF_DEBUG
		Serial.println("Receieved new action!");
#endif // AFF_DEBUG
		FirebaseObject event = Firebase.readEvent();
		if (event.getString("type") == "put")
		{
			initializeHubAction();
			hubAction.type = event.getInt("data/type");
			//hubAction.rawData = event.getString("data/rawData");
			hubAction.rawLen = event.getInt("data/rawLen");
			hubAction.sender = event.getString("data/sender");
			hubAction.timestamp = event.getString("data/timestamp");
			hubAction.repeat = event.getBool("data/repeat");
			if (hubAction.type == IR_ACTION_SEND)
			{
				readRawData(getCorrectedChunkCount(hubAction.rawLen));
			}
			return true; 
		}
	}
	
	return false;
}

/**	Sends an Error Result with type errorType to firebase. **/
void ArduinoFirebaseFunctions::sendError(const int errorType)
{
	initializeHubResult();
	hubResult.resultCode = errorType;
	hubResult.timestamp = String(millis());

	FirebaseObject obj = FirebaseObject(parseHubResultToJson().c_str());
	sendToFirebase(ResultPath, obj.getJsonVariant("/"));
}

/**
 *
 **/
void ArduinoFirebaseFunctions::sendRecordedSignal(const decode_results& results)
{
#ifdef AFF_DEBUG
	Serial.println("Setting up hubResult...");
#endif // AFF_DEBUG

	initializeHubResult();
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.encoding = results.decode_type; //typeToString(results.decode_type, results.repeat);
	hubResult.code = "0x" + resultToHexidecimal(results);
	hubResult.timestamp = String(millis());
	//hubResult.rawData = rawDataToString(results);
	hubResult.rawLen = getCorrectedRawLength(results);
	String resStr = parseHubResultToJson();

#ifdef AFF_DEBUG
	Serial.println(resStr);
	Serial.print("to ");
	Serial.println(ResultPath);
#endif // AFF_DEBUG

	FirebaseObject obj = FirebaseObject(resStr.c_str());
	sendToFirebase(ResultPath, obj.getJsonVariant("/"));
	sendRawData(results);
}


/*	------------------
 *	Private Functions
 *	------------------ 
 */

/**
 *	Begins reading raw data chunks until all raw data has been
 *	received or a timeout occurs. On successful completetion,
 *	a RES_SEND_SUCC result is sent. On timeout, an ERR_TIMEOUT
 *	result is sent. 
 *
 *	Note: This function assumes hubAction has been initialized
 *	prior to calling.
**/
void ArduinoFirebaseFunctions::readRawData(uint16_t numChunks)
{
	int chunksReceived = 0;
	long unsigned startTime = millis();
	String chunk;
	String path = BasePath + "/rawData/" + chunksReceived;
	uint16_t* marker;

	
#ifdef AFF_DEBUG
	if (hubAction.rawData != NULL)
	{
		Serial.println("WARNING: rawData was not NULL at start of readRawData()...");
	}
#endif // AFF_DEBUG

	// Allocate memory for rawData array
	hubAction.rawData = (uint16_t *) calloc(hubAction.rawLen, sizeof(uint16_t));
	marker = hubAction.rawData;

#ifdef AFF_DEBUG
	Serial.print("Reading in ");
	Serial.print(numChunks);
	Serial.println(" chunks...");
#endif // AFF_DEBUG

	while (chunksReceived < numChunks && millis() - startTime < READ_RAW_DATA_TIMEOUT)
	{
		// Read chunks
		chunk = Firebase.getString(path);
		if (!Firebase.failed())
		{

			marker = parseRawDataString(chunk.c_str(), hubAction.rawData, chunksReceived * CHUNK_SIZE);
			path = BasePath + "/rawData/" + ++chunksReceived;
		}
	}

#ifdef AFF_DEBUG
	for (int i = 0; i < numChunks; i++)
	{
		Serial.print("rawData["); Serial.print(i); Serial.print("]: ");
		Serial.println(rawDataToString(hubAction.rawData, hubAction.rawLen, i * CHUNK_SIZE, true));
	}
#endif // AFF_DEBUG

	// Send err if chunksReceived != numChunks
	if (chunksReceived != numChunks)
	{
#ifdef AFF_DEBUG
		Serial.println("Didn't received all the chunks in alloted time.");
#endif // AFF_DEBUG
		sendError(ERR_TIMEOUT);
	}
	else
	// Otherwise send success result
	{
		String end = "}";
		String resStr = "{\"resultCode\": " + String(RES_SEND_SUCC) + end;
		FirebaseObject obj = FirebaseObject(resStr.c_str());
#ifdef AFF_DEBUG
		Serial.print("Sending: "); Serial.println(resStr);
#endif // AFF_DEBUG
		sendToFirebase(ResultPath, obj.getJsonVariant("/"));
		sendStringToFirebase(BasePath + "/rawData", "_none_");
	}
}

/**
 *	Sends raw data in chunks of 50 words at a time. The first thing sent is
 *	the number of chunks, followed by each chunk with its position in the
 *	array.
**/
void ArduinoFirebaseFunctions::sendRawData(const decode_results& results)
{
	String path = BasePath + "/rawData";
	int numChunks = getCorrectedChunkCount(hubResult.rawLen);

//	String tempStr = "{\"numChunks\": " + String(numChunks) + "}";
//
//#ifdef AFF_DEBUG
//	Serial.print("numChunksStr: ");
//	Serial.println(tempStr);
//#endif // AFF_DEBUG
//
//	FirebaseObject numChunkObj = FirebaseObject(tempStr.c_str());
//	sendToFirebase(path, numChunkObj.getJsonVariant("/"));

	for (int i = 0; i < numChunks; i++)
	{
		String rawDataStr = rawDataToString(results.rawbuf, results.rawlen, (i * CHUNK_SIZE) + 1, true);

#ifdef AFF_DEBUG
		Serial.print("Sending: ");
		Serial.println(rawDataStr);
#endif // AFF_DEBUG
		sendStringToFirebase(path + "/" + i, rawDataStr);
	}

#ifdef AFF_DEBUG
	Serial.println("Done!");
#endif // AFF_DEBUG
}


/**
 *	Continually tries to send the string message to the given path until it either
 *	succeeds or exceeded maxRetries.
**/
void ArduinoFirebaseFunctions::sendStringToFirebase(const String& path, const String& message)
{
	for (int i = 0; i < maxRetries; i++)
	{
		Firebase.setString(path, message);

		if (Firebase.failed())
		{
#ifdef AFF_DEBUG
			Serial.print("Failed to send... (");
			Serial.print(i + 1);
			Serial.print("/");
			Serial.print(maxRetries);
			Serial.println(")");
#endif // AFF_DEBUG
			delay(FAILED_DELAY);
		}
		else
		{
			return;
		}
	}
}

/**
 *	Continually tries to send the json object to the given path until it either
 *	succeeds or exceeded maxRetries.
**/
void ArduinoFirebaseFunctions::sendToFirebase(const String& path, const JsonVariant& obj)
{
	for (int i = 0; i < maxRetries; i++)
	{
		Firebase.set(path, obj);

		if (Firebase.failed())
		{
#ifdef AFF_DEBUG
			Serial.print("Failed to send... (");
			Serial.print(i + 1);
			Serial.print("/");
			Serial.print(maxRetries);
			Serial.println(")");
#endif // AFF_DEBUG
			delay(FAILED_DELAY);
		} 
		else
		{
			return;
		}
	}
}

/** 
 *	Convert the result's value/state to simple hexadecimal. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266) 
**/
String ArduinoFirebaseFunctions::resultToHexidecimal(const decode_results& result)
{
	String output = "";
//	if (hasACState(result.decode_type)) 
//	{
//#if DECODE_AC
//		for (uint16_t i = 0; result.bits > i * 8; i++) {
//			if (result.state[i] < 0x10) output += '0';  // Zero pad
//			output += uint64ToString(result.state[i], 16);
//		}
//#endif
//	}
//	else 
	//{
		output += uint64ToString(result.value, 16);
	//}

	return output;
}

/**
 *	Return the corrected length of a 'raw' format array structure after over-large values are
 *	converted into multiple entries. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
**/
uint16_t ArduinoFirebaseFunctions::getCorrectedRawLength(const decode_results& results) 
{
	uint16_t extended_length = results.rawlen - 1;
	for (uint16_t i = 0; i < results.rawlen - 1; i++) 
	{
		uint32_t usecs = results.rawbuf[i] * kRawTick;
		// Add two extra entries for multiple larger than UINT16_MAX it is.
		extended_length += (usecs / (UINT16_MAX + 1)) * 2;
	}

	return extended_length;
}

void ArduinoFirebaseFunctions::getNextWord(const char*& startWord, const char*& endWord, int& startWordPos, int& endWordPos)
{
	while (*startWord != '\0' && *startWord != '\"') { startWordPos++; startWord++; }
	endWord = ++startWord;
	endWordPos = ++startWordPos;
	while (*endWord != '\0' && *endWord != '\"') { endWordPos++; endWord++; }
}

void ArduinoFirebaseFunctions::getNextNumber(const char*& startWord, const char*& endWord, int& startWordPos, int& endWordPos)
{
	while (*startWord != '\0' && (*startWord < '0' || *startWord > '9')) { startWordPos++; startWord++; }
	endWord = startWord;
	endWordPos = startWordPos;
	while (*endWord != '\0' && *endWord != ',' && *endWord != '}') { endWordPos++; endWord++; }
}

/**
 *	Handles the parsing logic for a HubAction.
**/
void ArduinoFirebaseFunctions::parseJsonToHubAction(const String jsonStr)
{
	const char* startWord = jsonStr.begin();
	const char* endWord = jsonStr.begin();
	int startWordPos = 0;
	int endWordPos = 0;
	String key;
	String strValue;
	uint16_t longValue;
	int intValue;


	while (*startWord != '\0' && *endWord != '\0')
	{
		getNextWord(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
		key = jsonStr.substring(startWordPos, endWordPos);
#ifdef AFF_DEBUG
		Serial.print("Key = "); Serial.println(key);
#endif // AFF_DEBUG
		startWord = endWord + 1;
		startWordPos = endWordPos + 1;

		if (key == F("repeat"))
		{
			getNextNumber(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			intValue = jsonStr.substring(startWordPos, endWordPos).toInt();
#ifdef AFF_DEBUG
			Serial.print("repeat = "); Serial.println(intValue, DEC);
#endif // AFF_DEBUG
			hubAction.repeat = intValue;
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
		if (key == F("timestamp"))
		{
			getNextWord(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			strValue = jsonStr.substring(startWordPos, endWordPos);
#ifdef AFF_DEBUG
			Serial.print("timestamp = "); Serial.println(strValue);
#endif // AFF_DEBUG
			hubAction.timestamp = String(strValue);
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
		else if (key == F("sender"))
		{
			getNextWord(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			strValue = jsonStr.substring(startWordPos, endWordPos);
#ifdef AFF_DEBUG
			Serial.print("sender = "); Serial.println(strValue);
#endif // AFF_DEBUG
			hubAction.sender = String(strValue);
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
//		else if (key == F("rawData"))
//		{
//			getNextWord(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
//			strValue = jsonStr.substring(startWordPos, endWordPos);
//#ifdef AFF_DEBUG
//			Serial.print("rawData = "); Serial.println(strValue);
//#endif // AFF_DEBUG
//			hubAction.rawData = String(strValue);
//			startWord = ++endWord;
//			startWordPos = ++endWordPos;
//		}
		else if (key == F("rawLen"))
		{
			getNextNumber(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			longValue = atol(jsonStr.substring(startWordPos, endWordPos).c_str());
#ifdef AFF_DEBUG
			Serial.print("rawLen = "); Serial.println(longValue, DEC);
#endif // AFF_DEBUG
			hubAction.rawLen = longValue;
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
		else if (key == F("type"))
		{
			getNextNumber(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			intValue = jsonStr.substring(startWordPos, endWordPos).toInt();
#ifdef AFF_DEBUG
			Serial.print("type = "); Serial.println(intValue, DEC);
#endif // AFF_DEBUG
			hubAction.type = intValue;
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
	}
}

String ArduinoFirebaseFunctions::parseHubActionToJson()
{
	//String repeat = (hubResult.repeat) ? F("1}") : F("0}");
	String retStr = "{" +  HR_STR_SENDER
		+ hubAction.sender + HR_STR_TIMESTAMP
		+ hubAction.timestamp + HR_STR_TYPE
		+ hubAction.type + HR_STR_RAW_LEN
		+ hubAction.rawLen + HR_STR_REPEAT;
	if (hubAction.repeat)
		retStr += "1}";
	else
		retStr += "0}";

	return retStr;
}

String ArduinoFirebaseFunctions::parseHubResultToJson()
{
	String repeat = (hubResult.repeat) ? "1}" : "0}";
	String retStr = HR_STR_RES_CODE 
		+ String(hubResult.resultCode) + HR_STR_CODE
		+ hubResult.code + HR_STR_TIMESTAMP
		+ hubResult.timestamp + HR_STR_ENCODING
		+ hubResult.encoding + HR_STR_RAW_LEN
		+ hubResult.rawLen + HR_STR_REPEAT
		+ repeat;

	return retStr;
}

/**
  *	Sets all the values of HubAction to initial values.
 **/
void ArduinoFirebaseFunctions::initializeHubAction()
{
	if (hubAction.rawData != NULL)
		free(hubAction.rawData);

	hubAction.type = 0;
	hubAction.rawData = NULL;
	hubAction.rawLen = 0;
	hubAction.sender = "_none_";
	hubAction.timestamp = "_none_";
	hubAction.repeat = false;
}

/**
  *	Sets all the values of HubResult to initial values.
 **/
void ArduinoFirebaseFunctions::initializeHubResult()
{
	hubResult.code = "_none_";
	hubResult.encoding = 0;
	hubResult.rawData = "_none_";
	hubResult.rawLen = 0;
	hubResult.timestamp = "_none_";
	hubResult.resultCode = 0;
	hubResult.repeat = false;
}

/*	--------------------
 *	 Static Functions
 *	--------------------
*/

#ifndef IR_DEBUG_IR_FUNC
/**
 *	Converts a uint64_t to a string. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
**/
String uint64ToString(uint64_t input, uint8_t base)
{
	String result = "";
	// Check we have a base that we can actually print.
	// i.e. [0-9A-Z] == 36
	if (base < 2 || base > 36) base = 10;

	do
	{
		char c = input % base;
		input /= base;

		c += c < 10 ? '0' : 'A' - 10;

		result = c + result;
	} while (input);

	return result;
}
#endif // !IR_DEBUG_IR_FUNC



/**
  *	Converts the raw data from array of uint16_t to a string.
  * Note: Trying to convert more than CHUNK_SIZE could lead to
  *	memory instability.
 **/
String rawDataToString(volatile uint16_t* rawbuf, uint16_t rawLen, uint16_t startPos, bool limitToChunk)
{
	String output = "";
	// Dump data

	uint32_t usecs;
	for (uint16_t i = startPos; i < rawLen && (i - startPos < CHUNK_SIZE || !limitToChunk); i++)
	{
		// If data is > UINT16_MAX, add multiple entries
		for (usecs = rawbuf[i] * kRawTick; usecs > UINT16_MAX; usecs -= UINT16_MAX)
		{
			output += uint64ToString(UINT16_MAX);
			if (i % 2)
				output += F(", 0,  ");
			else
				output += F(",  0, ");
		}
		output += uint64ToString(usecs, 10);
		if (i < rawLen - 1)
			output += F(", ");						// ',' not needed on the last one
		if (i % 2 == 0)
			output += ' ';							// Extra if it was even.
	}

	// End declaration
	output += F("\0");

	return output;
}

/**
  *	Gets the number of chunks needed based on the 
  *	length of the rawData array. This function
  *	always rounds up to ensure enough chunks are
  *	allocated.
 **/
uint16_t getCorrectedChunkCount(uint16_t rawLen)
{
	uint16_t count = ceil(rawLen / CHUNK_SIZE);

	return count * CHUNK_SIZE < rawLen ? count + 1 : count;
}

/**
  *	Converts a string of number into an array of numbers and places
  *	the array into rawData. Returns a pointer to the next open spot
  * in rawData array. 
  *
  *	Note: This function assumes there is enough memory allocated to
  *	rawData to hold the amount of numbers found in dataStr.
 **/
uint16_t* parseRawDataString(const char* dataStr, uint16_t* rawData, uint16_t startPos)
{
	// Next free position in rawData array
	uint16_t rawDataPos = startPos;

	// Points to next char to parse
	char* pointer = (char*)dataStr;

	// Debug statement
#ifdef AFF_DEBUG_PARSE
	Serial.print("Parsing: ");
	Serial.println(dataStr);
#endif

	// Continue parsing until reach end of dataStr array
	while (*pointer != '\0')
	{
		// Skip values that aren't numbers
		if (*pointer < '0' || *pointer > '9')
		{
			pointer++;
			continue;
		}

#ifdef AFF_DEBUG_PARSE
		Serial.print("Setting rawData["); 
		Serial.print(rawDataPos);
		Serial.print("] = ");
		//Serial.println(strtol(pointer, &pointer, 10));
#endif // AFF_DEBUG_PARSE

		// Set number in array
		rawData[rawDataPos++] = strtol(pointer, &pointer, 10);

#ifdef AFF_DEBUG_PARSE
		Serial.println(rawData[rawDataPos - 1]);
#endif // AFF_DEBUG_PARSE
	}

	// Debug statement
#ifdef AFF_DEBUG_PARSE
	Serial.println("");
#endif

	// Return next spot in rawData array
	return rawData + rawDataPos;
}


/*	------------------
 *	 Unit Tests 
 *	------------------ 
*/

#ifdef IRSMARTHUB_UNIT_TESTS
int ArduinoFirebaseFunctions::test_parseHubResultToJson()
{
	initializeHubResult();

	// Test Send Signal One Chunk Result
	hubResult.code = "0x05";
	hubResult.encoding = 7;
	hubResult.rawData = "This is raw data";
	hubResult.rawLen = 2;
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.timestamp = "1-1-1";
	hubResult.repeat = true;

	String str = parseHubResultToJson();
	String expectedStr = "{\"resultCode\": 700, \"code\": \"0x05\", \"timestamp\": \"1-1-1\", \"encoding\": 7, \"rawLen\": 2, \"repeat\": 1}";
	bool bPassed = str == expectedStr;
	int numFailed = bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseHubResultToJson failed (send result): Expected " + expectedStr + " but actually" + str;
		Serial.println(failedStr);
	}

	// Test Send Signal Multiple Chunks Result
	hubResult.code = "0x05";
	hubResult.encoding = 2;
	hubResult.rawData = "This is raw data";
	hubResult.rawLen = 52;
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.timestamp = "1-1-1";
	hubResult.repeat = true;

	str = parseHubResultToJson();
	expectedStr = "{\"resultCode\": 700, \"code\": \"0x05\", \"timestamp\": \"1-1-1\", \"encoding\": 2, \"rawLen\": 52, \"repeat\": 1}";
	bPassed = str == expectedStr;
	numFailed += bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseHubResultToJson failed (send result): Expected " + expectedStr + " but actually" + str;
		Serial.println(failedStr);
	}

	// Test Timeout Error Result
	initializeHubResult();
	hubResult.resultCode = ERR_TIMEOUT;
	hubResult.timestamp = "1-3-1";

	str = parseHubResultToJson();
	expectedStr = "{\"resultCode\": 801, \"code\": \"_none_\", \"timestamp\": \"1-3-1\", \"encoding\": 0, \"rawLen\": 0, \"repeat\": 0}";
	bPassed = str == expectedStr;
	numFailed += bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseHubResultToJson failed (timeout error): Expected " + expectedStr + " but actually " + str;
		Serial.println(failedStr);
	}

	return numFailed;
}

int ArduinoFirebaseFunctions::test_parseHubActionToJson()
{
	// Test Learn Action
	initializeHubAction();
	hubAction.type = IR_ACTION_LEARN;
	hubAction.sender = "THE SENDER";
	hubAction.rawLen = 34;
	hubAction.timestamp = "1-2-3";
	hubAction.repeat = true;

	String str = parseHubActionToJson();
	String expectedStr = "{\"sender\": \"THE SENDER\", \"timestamp\": \"1-2-3\", \"type\": 1, \"rawLen\": 34, \"repeat\": 1}";
	bool bPassed = str == expectedStr;
	int numFailed = bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseHubActionToJson failed (learn action): Expected " + expectedStr + " but actually " + str;
		Serial.println(failedStr);
	}

	// Test initialization (none action)
	initializeHubAction();

	str = parseHubActionToJson();
	expectedStr = "{\"sender\": \"_none_\", \"timestamp\": \"_none_\", \"type\": 0, \"rawLen\": 0, \"repeat\": 0}";
	bPassed = str == expectedStr;
	numFailed += bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedString = "parseHubActionToJson failed (initialization): Expected " + expectedStr + " but actually " + str;
		Serial.println(failedString);
	}

	return numFailed;
}

int ArduinoFirebaseFunctions::test_parseJsonToHubAction()
{
	initializeHubAction();

	// Test Learn Signal
	parseJsonToHubAction(F("{\"type\": 2, \"timestamp\": \"1-2-2\", \"rawLen\": 1, \"sender\": \"Sender\", \"repeat\": 0}"));

	bool bPassed = hubAction.type == 2 && hubAction.timestamp == "1-2-2" && hubAction.rawLen == 1 && hubAction.sender == "Sender" && hubAction.repeat == false;
	int numFailed = bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseJsonToHubAction failed (learn signal): ";
		if (hubAction.type != 2)
			failedStr += "<type = " + String(hubAction.type) + " instead of 2> ";
		if (hubAction.timestamp != "1-2-2")
			failedStr += "<timestamp = \"" + hubAction.timestamp + "\" instead of \"1-2-2\"> ";
		if (hubAction.rawLen != 1)
			failedStr += "<rawLen = " + String(hubAction.rawLen) + " instead of 1> ";
		if (hubAction.sender != "Sender")
			failedStr += "<sender = \"" + hubAction.sender + "\" instead of \"Sender\">";
		if (hubAction.repeat != false) 
		{
			failedStr += "<repeat = ";
			if (hubAction.repeat)
				failedStr += "TRUE";
			else
				failedStr += "FALSE";
			failedStr += " instead of FALSE";
		}
	
		Serial.println(failedStr);
	}

	return numFailed;
}
#endif
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H