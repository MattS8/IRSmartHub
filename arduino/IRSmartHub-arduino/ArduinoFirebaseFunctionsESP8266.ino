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

	// Reconnect whenever we lose connection
	Firebase.reconnectWiFi(true);

	// Extra safe initialization of HubAction
	initializeHubAction();

	// Set initial action to NONE
	if (!Firebase.setJSON(firebaseWriteData, ActionPath, parseHubActionToJson()))
	{
#ifdef AFF_DEBUG
		Serial.println(F("Failed to set initial action..."));
		ESP.reset();
#endif // AFF_DEBUG
	}

	//delay(2000);
	//hubAction.rawData = "rawData3";
	//hubAction.rawLen = 3;
	//hubAction.sender = "ME3";
	//hubAction.timestamp = "3-3-3";
	//Firebase.setJSON(firebaseWriteData, ResultPath, "{\"resultCode\": 700, \"code\": \"0xE0E040BF\", \"timestamp\": \"44534\", \"encoding\": \"7\", \"rawData\": \"4480, 4492, 574, 1660, 576, 1658, 600, 1636, 594, 532, 602, 526, 574, 598, 536, 592, 530, 558, 576, 1658, 576, 1656, 580, 1658, 598, 528, 596, 532, 604, 526, 576, 594, 532, 556, 578, 552, 572, 1660, 572, 558, 576, 594, 532, 556, 576, 554, 596, 532, 570, 558, 602, 1632, 572, 558, 574, 1700, 532, 1660, 570, 1702, 536, 1696, 532, 1660, 602, 1632, 574, 46606, 4518, 4476, 596, 1634, 602, 1630, 604, 1630, 576, 594, 530, 558, 602, 526, 578, 554, 576, 594, 534, 1658, 604, 1628, 604, 1630, 576, 552, 578, 550, 574, 598, 534, 554, 572, 556, 572, 556, 578, 1696, 532, 556, 576, 552, 604, 526, 574, 554, 578, 552, 578, 552, 574, 1700, 536, 552, 576, 1698, 536, 1698, 532, 1660, 602, 1628, 580, 1696, 538, 1656, 600\", \"rawLen\": 135, \"repeat\": 0}");

	//delay(2000);
	//hubAction.rawData = "rawData4";
	//hubAction.rawLen = 4;
	//hubAction.sender = "ME4";
	//hubAction.timestamp = "4-4-4";
	//Firebase.setJSON(firebaseWriteData, ActionPath, parseHubActionToJson());

	//delay(2000);
	//hubAction.rawData = "rawData5";
	//hubAction.rawLen = 5;
	//hubAction.sender = "ME5";
	//hubAction.timestamp = "5-5-5";
	//Firebase.setJSON(firebaseWriteData, ActionPath, parseHubActionToJson());

	//delay(2000);
	//hubResult.rawData = "4516, ,  602, 1634,  602, 528,  600, 530,  600, 528,  602, 526,  598, 534,  600, 1632,  600, 1634,  596, 1636,  598, 532,  598, 532,  598, 530,  600, 530,  600, 530,  598, 530,  596, 1636,  598, 532,  600, 530,  596, 532,  598, 532,  598, 530,  598, 532,  596, 1636,  596, 532,  600, 1632,  598, 1636,  600, 1632,  600, 1634,  600, 1632,  598, 1634,  602, 46586,  4518, 4472,  604, 1628,  598, 1634,  600, 1634,  602, 528,  600, 530,  598, 530,  600, 530,  602, 528,  598, 1634,  596, 1636,  598, 1678,  556, 530,  598, 530,  600, 532,  596, 532,  598, 532,  598, 530,  598, 1634,  600, 530,  602, 528,  600, 530,  600, 530,  600, 528,  600, 530,  598, 1636,  598, 530,  598, 1634,  596, 1640,  598, 1632,  596, 1636,  602, 1632,  602, 1628,  602";
	//hubResult.rawLen = 135;
	//hubResult.encoding = "7";
	//hubResult.timestamp = "24881";
	//hubResult.resultCode = RES_SEND_SIG;
	//hubResult.code = "0xE0E040BF";
	//Firebase.setJSON(firebaseWriteData, ResultPath, parseHubResultToJson());

	//Firebase.setJSON(firebaseWriteData, ResultPath, "{\"resultCode\": 700, \"code\": \"0xE0E040BF\", \"timestamp\": \"44534\", \"encoding\": \"7\", \"rawData\": \"4480, 4492, 574, 1660, 576, 1658, 600, 1636, 594, 532, 602, 526, 574, 598, 536, 592, 530, 558, 576, 1658, 576, 1656, 580, 1658, 598, 528, 596, 532, 604, 526, 576, 594, 532, 556, 578, 552, 572, 1660, 572, 558, 576, 594, 532, 556, 576, 554, 596, 532, 570, 558, 602, 1632, 572, 558, 574, 1700, 532, 1660, 570, 1702, 536, 1696, 532, 1660, 602, 1632, 574, 46606, 4518, 4476, 596, 1634, 602, 1630, 604, 1630, 576, 594, 530, 558, 602, 526, 578, 554, 576, 594, 534, 1658, 604, 1628, 604, 1630, 576, 552, 578, 550, 574, 598, 534, 554, 572, 556, 572, 556, 578, 1696, 532, 556, 576, 552, 604, 526, 574, 554, 578, 552, 578, 552, 574, 1700, 536, 552, 576, 1698, 536, 1698, 532, 1660, 602, 1628, 580, 1696, 538, 1656, 600\", \"rawLen\": 135, \"repeat\": 0}");
	//delay(2000);


	if (!Firebase.beginStream(firebaseReadData, ActionPath))
	{
#ifdef AFF_DEBUG
		Serial.println(DEBUG_DIV);
		Serial.println(F("Can't begin stream connection..."));
		Serial.println(DEBUG_DIV);
		Serial.println();
#endif // AFF_DEBUG
		ESP.reset();
	}

	//delay(4000);
	//Serial.println("here we go...");
	//hubResult.rawData = "";
	////hubResult.rawData = "4516, 4470,  602, 1632,  602, 1630,  600, 1634,  602, 528,  600, 530,  600, 528,  602, 526,  598, 534,  600, 1632,  600, 1634,  596, 1636,  598, 532,  598, 532,  598, 530,  600, 530,  600, 530,  598, 530,  596, 1636,  598, 532,  600, 530,  596, 532,  598, 532,  598, 530,  598, 532,  596, 1636,  596, 532,  600, 1632,  598, 1636,  600, 1632,  600, 1634,  600, 1632,  598, 1634,  602, 46586,  4518, 4472,  604, 1628,  598, 1634,  600, 1634,  602, 528,  600, 530,  598, 530,  600, 530,  602, 528,  598, 1634,  596, 1636,  598, 1678,  556, 530,  598, 530,  600, 532,  596, 532,  598, 532,  598, 530,  598, 1634,  600, 530,  602, 528,  600, 530,  600, 530,  600, 528,  600, 530,  598, 1636,  598, 530,  598, 1634,  596, 1640,  598, 1632,  596, 1636,  602, 1632,  602, 1628,  602";
	//hubResult.rawLen = 135;
	//hubResult.encoding = "7";
	//hubResult.timestamp = "24881";
	//hubResult.resultCode = RES_SEND_SIG;
	//hubResult.code = "0xE0E040BF";
	//String temp = "{resultCode: 700, code: 0xE0E040BF, timestamp: 44534, encoding: 7, rawData: 5, rawLen: 135, repeat: 0}";
	//Serial.println(temp);
	//if (!Firebase.setJSON(firebaseWriteData, ResultPath, temp))//"{\"resultCode\": 700, \"code\": \"0xE0E040BF\", \"timestamp\": \"44534\", \"encoding\": \"7\", \"rawData\": \"4480, 4492, 574, 1660, 576, 1658, 600, 1636, 594, 532, 602, 526, 574, 598, 536, 592, 530, 558, 576, 1658, 576, 1656, 580, 1658, 598, 528, 596, 532, 604, 526, 576, 594, 532, 556, 578, 552, 572, 1660, 572, 558, 576, 594, 532, 556, 576, 554, 596, 532, 570, 558, 602, 1632, 572, 558, 574, 1700, 532, 1660, 570, 1702, 536, 1696, 532, 1660, 602, 1632, 574, 46606, 4518, 4476, 596, 1634, 602, 1630, 604, 1630, 576, 594, 530, 558, 602, 526, 578, 554, 576, 594, 534, 1658, 604, 1628, 604, 1630, 576, 552, 578, 550, 574, 598, 534, 554, 572, 556, 572, 556, 578, 1696, 532, 556, 576, 552, 604, 526, 574, 554, 578, 552, 578, 552, 574, 1700, 536, 552, 576, 1698, 536, 1698, 532, 1660, 602, 1628, 580, 1696, 538, 1656, 600\", \"rawLen\": 135, \"repeat\": 0}"))
	//{
	//	Serial.println(firebaseWriteData.errorReason());
	//}
	
}

/**
 *	Sets the name of the hub in firebase.
 **/
void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	String namePath = BasePath + "/name";
	if (!Firebase.setString(firebaseWriteData, namePath, name))
	{
#ifdef AFF_DEBUG
		Serial.println("Failed to setup name...");
#endif // AFF_DEBUG
	}
}

/**
 *	Polls firebaseReadData to see if new data has
 *	been received from backend. Returns whether
 *	a good connection is established, regardless
 *	of whether or not new information has been
 *	read.
**/
bool ArduinoFirebaseFunctions::readStreamData()
{
	bool bSuccess = Firebase.readStream(firebaseReadData);

#ifdef AFF_DEBUG
	if (!bSuccess)
	{
		Serial.println(F("Can't read stream data"));
		Serial.println();
	}
#endif // AFF_DEBUG

	return bSuccess;
}

/**
 *	I guess this checks for a connection timeout
 *	and possibly re-establishes connection? Need 
 *	to look at the source code to fully understand
 *	why this line was needed in the examples.
**/
bool ArduinoFirebaseFunctions::streamTimeout()
{
	bool bSuccess = firebaseReadData.streamTimeout();
#ifdef AFF_DEBUG
#endif //AFF_DEBUG

	return bSuccess;
}

/**
 *	Determines if new data has been received from
 *	Firebase. If so, the data is parsed into hubAction
 *	and returns true. Otherwise, false is returned.
 *
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
			Serial.print(F("firebaseReadData was not a JSON object but of type: "));
			Serial.println(firebaseReadData.dataType());
#endif //AFF_DEBUG
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
	if (!Firebase.setJSON(firebaseWriteData, ResultPath, parseHubResultToJson()))
	{
#ifdef AFF_DEBUG
		Serial.println("Failed to send error...");
#endif // AFF_DEBUG
	}
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
	hubResult.rawData = rawDataToString(results);
	hubResult.rawLen = getCorrectedRawLength(results);
	String resStr = parseHubResultToJson();

#ifdef AFF_DEBUG
	Serial.println(resStr);
	Serial.print("to ");
	Serial.println(ResultPath);
#endif // AFF_DEBUG

	if (!Firebase.setJSON(firebaseWriteData, ResultPath, parseHubResultToJson()))
	{
#ifdef AFF_DEBUG
		Serial.println("Failed to send recorded signal...");
		Serial.println(firebaseWriteData.errorReason());
#endif // AFF_DEBUG
	}
}


/* ---------- Private Functions ---------- */

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
String ArduinoFirebaseFunctions::rawDataToString(const decode_results& results)
{
	String output = "";
	// Dump data
	for (uint16_t i = 1; i < results.rawlen; i++) 
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
#ifdef AFF_DEBUG
		Serial.println(DEBUG_DIV);
#endif// AFF_DEBUG
	}
}

String ArduinoFirebaseFunctions::parseHubActionToJson()
{
	//String repeat = (hubResult.repeat) ? F("1}") : F("0}");
	String retStr = F("{") +  HR_STR_SENDER
		+ hubAction.sender + HR_STR_TIMESTAMP
		+ hubAction.timestamp + HR_STR_TYPE
		+ hubAction.type + HR_STR_RAW_DATA
		+ hubAction.rawData + HR_STR_RAW_LEN
		+ hubAction.rawLen + HR_STR_REPEAT;
	if (hubAction.repeat)
		retStr += F("1}");
	else
		retStr += F("0}");

	return retStr;
}

String ArduinoFirebaseFunctions::parseHubResultToJson()
{
	String repeat = (hubResult.repeat) ? F("1}") : F("0}");
	String retStr = HR_STR_RES_CODE 
		+ String(hubResult.resultCode) + HR_STR_CODE
		+ hubResult.code + HR_STR_TIMESTAMP
		+ hubResult.timestamp + HR_STR_ENCODING
		+ hubResult.encoding + "\"" + HR_STR_RAW_DATA
		+ hubResult.rawData + HR_STR_RAW_LEN
		+ hubResult.rawLen + HR_STR_REPEAT + repeat;

	return retStr;
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

	// Test Send Signal Result
	hubResult.code = "0x05";
	hubResult.encoding = "SAMSUNG";
	hubResult.rawData = "This is raw data";
	hubResult.rawLen = 2;
	hubResult.resultCode = RES_SEND_SIG;
	hubResult.timestamp = "1-1-1";
	hubResult.repeat = true;

	String str = parseHubResultToJson();
	String expectedStr = "{\"resultCode\": 700, \"code\": \"0x05\", \"timestamp\": \"1-1-1\", \"encoding\": \"SAMSUNG\", \"rawData\": \"This is raw data\", \"rawLen\": 2, \"repeat\": 1}";
	bool bPassed = str == expectedStr;
	int numFailed = bPassed ? 0 : 1;

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
	expectedStr = "{\"resultCode\": 801, \"code\": \"_none_\", \"timestamp\": \"1-3-1\", \"encoding\": \"_none_\", \"rawData\": \"_none_\", \"rawLen\": 0, \"repeat\": 0}";
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
	parseJsonToHubAction(F("{\"type\": 2, \"timestamp\": \"1-2-2\", \"rawData\": \"_none_\", \"rawLen\": 1, \"sender\": \"Sender\", \"repeat\": 0}"));

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