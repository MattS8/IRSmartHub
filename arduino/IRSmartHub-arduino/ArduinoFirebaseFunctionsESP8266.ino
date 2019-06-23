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
			hubAction.rawData = event.getString("data/rawData");
			hubAction.rawLen = event.getInt("data/rawLen");
			hubAction.sender = event.getString("data/sender");
			hubAction.timestamp = event.getString("data/timestamp");
			hubAction.repeat = event.getBool("data/repeat");
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
	hubResult.encoding = String(results.decode_type); //typeToString(results.decode_type, results.repeat);
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
 *	Sends raw data in chunks of 50 words at a time. The first thing sent is
 *	the number of chunks, followed by each chunk with its position in the
 *	array.
**/
void ArduinoFirebaseFunctions::sendRawData(const decode_results& results)
{
	String path = BasePath + "/rawData";
	int numChunks = getCorrectedChunkCount(hubResult.rawLen);

	String tempStr = "{\"numChunks\": " + String(numChunks) + "}";

#ifdef AFF_DEBUG
	Serial.print("numChunksStr: ");
	Serial.println(tempStr);
#endif // AFF_DEBUG

	FirebaseObject numChunkObj = FirebaseObject(tempStr.c_str());
	sendToFirebase(path, numChunkObj.getJsonVariant("/"));


	for (int i = 0; i < numChunks; i++)
	{
		String rawDataStr = rawDataToString(results, (i * CHUNK_SIZE) + 1);

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

/**
 *	Converts a uint64_t to a string. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
**/
String ArduinoFirebaseFunctions::uint64ToString(uint64_t input, uint8_t base)
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

/**
 *	Converts the raw data from results to a string. Used
 *  to store in Firebase. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
 **/
String ArduinoFirebaseFunctions::rawDataToString(const decode_results& results, uint16_t startPos)
{
	String output = "";
	// Dump data
	for (uint16_t i = startPos; i < results.rawlen && i < startPos + CHUNK_SIZE; i++) 
	{
		uint32_t usecs;
		for (usecs = results.rawbuf[i] * kRawTick; usecs > UINT16_MAX; usecs -= UINT16_MAX) 
		{
			output += uint64ToString(UINT16_MAX);
			if (i % 2)
				output += F(", 0,  ");
			else
			output += F(",  0, ");
		}
		output += uint64ToString(usecs, 10);
		if (i < results.rawlen - 1)
			output += F(", ");						// ',' not needed on the last one
		if (i % 2 == 0) 
			output += ' ';							// Extra if it was even.
	}

	// End declaration
	output += F("\0");

	return output;
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
		else if (key == F("rawData"))
		{
			getNextWord(startWord OUT, endWord OUT, startWordPos OUT, endWordPos OUT);
			strValue = jsonStr.substring(startWordPos, endWordPos);
#ifdef AFF_DEBUG
			Serial.print("rawData = "); Serial.println(strValue);
#endif // AFF_DEBUG
			hubAction.rawData = String(strValue);
			startWord = ++endWord;
			startWordPos = ++endWordPos;
		}
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
		+ hubAction.type + HR_STR_RAW_DATA
		+ hubAction.rawData + HR_STR_RAW_LEN
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
		+ hubResult.rawLen + HR_STR_DATA_CHUNKS
		+ getCorrectedChunkCount(hubResult.rawLen) + HR_STR_REPEAT
		+ repeat;

	return retStr;
}

uint16_t getCorrectedChunkCount(uint16_t rawLen)
{
	uint16_t count = ceil(rawLen / CHUNK_SIZE);

	return count * CHUNK_SIZE < rawLen ? count + 1 : count;
}

/**
 *	Sets all the values of HubAction to initial values.
**/
void ArduinoFirebaseFunctions::initializeHubAction()
{
	hubAction.type = 0;
	hubAction.rawData = "_none_";
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
	hubResult.encoding = "_none_";
	hubResult.rawData = "_none_";
	hubResult.rawLen = 0;
	hubResult.timestamp = "_none_";
	hubResult.resultCode = 0;
	hubResult.repeat = false;
}

/* ------------------ Unit Tests ------------------ */

#ifdef IRSMARTHUB_UNIT_TESTS
int ArduinoFirebaseFunctions::test_parseHubResultToJson()
{
	initializeHubResult();

	// Test Send Signal One Chunk Result
	hubResult.code = "0x05";
	hubResult.encoding = "SAMSUNG";
	hubResult.rawData = "This is raw data";
	hubResult.rawLen = 2;
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.timestamp = "1-1-1";
	hubResult.repeat = true;

	String str = parseHubResultToJson();
	String expectedStr = "{\"resultCode\": 700, \"code\": \"0x05\", \"timestamp\": \"1-1-1\", \"encoding\": \"SAMSUNG\", \"rawLen\": 2, \"numDataChunks\": 1, \"repeat\": 1}";
	bool bPassed = str == expectedStr;
	int numFailed = bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseHubResultToJson failed (send result): Expected " + expectedStr + " but actually" + str;
		Serial.println(failedStr);
	}

	// Test Send Signal Multiple Chunks Result
	hubResult.code = "0x05";
	hubResult.encoding = "SAMSUNG";
	hubResult.rawData = "This is raw data";
	hubResult.rawLen = 52;
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.timestamp = "1-1-1";
	hubResult.repeat = true;

	str = parseHubResultToJson();
	expectedStr = "{\"resultCode\": 700, \"code\": \"0x05\", \"timestamp\": \"1-1-1\", \"encoding\": \"SAMSUNG\", \"rawLen\": 52, \"numDataChunks\": 2, \"repeat\": 1}";
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
	expectedStr = "{\"resultCode\": 801, \"code\": \"_none_\", \"timestamp\": \"1-3-1\", \"encoding\": \"_none_\", \"rawLen\": 0, \"numDataChunks\": 0, \"repeat\": 0}";
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
	hubAction.rawData = "_none_";
	hubAction.rawLen = 34;
	hubAction.timestamp = "1-2-3";
	hubAction.repeat = true;

	String str = parseHubActionToJson();
	String expectedStr = "{\"sender\": \"THE SENDER\", \"timestamp\": \"1-2-3\", \"type\": 1, \"rawData\": \"_none_\", \"rawLen\": 34, \"repeat\": 1}";
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
	expectedStr = "{\"sender\": \"_none_\", \"timestamp\": \"_none_\", \"type\": 0, \"rawData\": \"_none_\", \"rawLen\": 0, \"repeat\": 0}";
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

	bool bPassed = hubAction.type == 2 && hubAction.timestamp == "1-2-2" && hubAction.rawData == "_none_" && hubAction.rawLen == 1 && hubAction.sender == "Sender" && hubAction.repeat == false;
	int numFailed = bPassed ? 0 : 1;

	if (!bPassed)
	{
		String failedStr = "parseJsonToHubAction failed (learn signal): ";
		if (hubAction.type != 2)
			failedStr += "<type = " + String(hubAction.type) + " instead of 2> ";
		if (hubAction.timestamp != "1-2-2")
			failedStr += "<timestamp = \"" + hubAction.timestamp + "\" instead of \"1-2-2\"> ";
		if (hubAction.rawData != " ")
			failedStr += "<rawData = \"" + hubAction.rawData + "\" instead of \" \"";
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