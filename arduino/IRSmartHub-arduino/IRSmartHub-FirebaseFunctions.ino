#include "IRSmartHub-FirebaseFunctions.h"
// Copyright 2019 Matthew Steinhardt

/** Forward Declarations **/
void handleTimeout(bool timeout);
void handleActionReceived(StreamData data);
void runAction();

/**	Connects to firebase endpoint and begins streaming. **/
#define CON_DEBUG
void IRSmartHubFirebaseFunctions::connect()
{
	// Start firebase connection
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
    Firebase.reconnectWiFi(true);

    //Set the size of WiFi rx/tx buffers in the case where we want to work with large data.
    // firebaseDataSEND.setBSSLBufferSize(1024, 1024);
    // firebaseDataRECV.setBSSLBufferSize(1024, 1024);

    //Set the size of HTTP response buffers in the case where we want to work with large data.
    // firebaseDataSEND.setResponseSize(1024);
    // firebaseDataRECV.setBSSLBufferSize(1024, 1024);

	// Extra safe initialization
	initializeHubAction();
    initializeHubResult();

 	// Set initial action to NONE
    bool success = sendAction();
	delay(1000);

	if (!success) {
        delay(2000);
        ESP.reset();
    }

    success = Firebase.beginStream(firebaseDataRECV, ActionPath);
    if (!success) {
        #ifdef CON_DEBUG
            Serial.println("------------------------------------");
            Serial.println("Can't begin stream connection...");
            Serial.println("REASON: " + firebaseDataRECV.errorReason());
            Serial.println("------------------------------------");
            Serial.println();
        #endif
        delay(2000);
        ESP.reset();
    }

    Firebase.setStreamCallback(firebaseDataRECV, handleActionReceived, handleTimeout);
 }


/**
 * Captures any timeout events that occur during streaming.
**/ 
#define TIMEOUT_DEBUG
void handleTimeout(bool timeout) 
{
    #ifdef TIMEOUT_DEBUG
        if (timeout) {
            Serial.println();
            Serial.println("Stream timeout, resume streaming...");
            Serial.println();
        }
    #endif

}

/**
 * Parses receieved data into an action that the hub can act on.
**/ 
#define HAR_DEBUG
void handleActionReceived(StreamData data) 
{
    if (data.dataType() == "json") {
        #ifdef HAR_DEBUG
            Serial.println("Stream data available...");
            Serial.println("STREAM PATH: " + data.streamPath());
            Serial.println("EVENT PATH: " + data.dataPath());
            Serial.println("DATA TYPE: " + data.dataType());
            Serial.println("EVENT TYPE: " + data.eventType());
            Serial.print("VALUE: ");
            printResult(data);
            Serial.println();
        #endif
        FirebaseJson *json = data.jsonObjectPtr();
        size_t len = json->iteratorBegin();
        String key, value = "";
        int type = 0;

        for (size_t i = 0; i < len; i++) {
            json->iteratorGet(i, type, key, value);
            if (key == "type") {
                hubAction.type = value.toInt();
            } else if (key == "rawLen") {
                hubAction.rawLen = value.toInt();
            } else if (key == "sender") {
                hubAction.sender = value;
            } else if (key == "timestamp") {
                hubAction.timestamp = value;
            } else if (key == "repeat") {
                hubAction.repeat = value.toInt();
            } else {
                #ifdef HAR_DEBUG
                Serial.println("Unexpected action response...");
                Serial.print("TYPE: ");
                Serial.println(type == FirebaseJson::JSON_OBJECT ? "object" : "array");
                Serial.print("KEY: ");
                Serial.println(key);
                #endif
            }
        }

        runAction();

    } else {
        #ifdef HAR_DEBUG
            Serial.print("Stream returned non-JSON response: ");
            Serial.println(data.dataType());
        #endif

        return;
    }
}

#define ACTION_DEBUG
void runAction() {
  switch (hubAction.type)
  {
  case IR_ACTION_LEARN:
    #ifdef ACTION_DEBUG
    Serial.println();
    Serial.println("Listening for new IR signal...");
    #endif
    //todo - start listening for new IR signal
    break;
  
  case IR_ACTION_SEND:
    #ifdef ACTION_DEBUG
    Serial.println();
    Serial.println("Sending IR signal...");
    #endif
    //todo - send ir signal (from rawData)
    break;

  case IR_ACTION_NONE:
    #ifdef ACTION_DEBUG
    Serial.println();
    Serial.println("Doing nothing...");
    #endif
    break;

  default:
    #ifdef ACTION_DEBUG
    Serial.println();
    Serial.print("Unknown action received (TYPE = ");
    Serial.print(hubAction.type);
    Serial.println(")");
    #endif
    break;
  }
}

/**
 * Attempts to send a HubAction to the normal
 * ActionPath. 
 * 
 * Returns TRUE if the action was successfully sent.
 * Returns FALSE if the action failed to send.
 * 
**/
bool IRSmartHubFirebaseFunctions::sendAction() {
    FirebaseJson json;
    json.add("sender", hubAction.sender);
    json.add("timestamp", hubAction.timestamp);
    json.add("type", hubAction.type);
    json.add("rawLen", hubAction.rawLen);
    json.add("repeat", hubAction.repeat ? 1 : 0);

    return sendToFirebase(ActionPath, json);
}


/**
 * Attempts to send a HubResult to the normal
 * ResultPath. 
 * 
 * Returns TRUE if the result was successfully sent.
 * Returns FALSE if the result failed to send.
 * 
**/
bool IRSmartHubFirebaseFunctions::sendResult() {
  FirebaseJson json;
  json.add("resultCode", hubResult.resultCode);
  json.add("code", hubResult.code);
  json.add("timestamp", hubResult.timestamp);
  json.add("encoding", hubResult.encoding);
  json.add("rawData", hubResult.rawData);
  json.add("rawLen", hubResult.rawLen);
  json.add("repeat", hubResult.repeat);

  return sendToFirebase(ResultPath, json);
}

/**
 * Attempts to send an error HubResult. This error object
 * can be found in the normal ResultPath. 
 * 
 * Returns TRUE if the error was successfully sent.
 * Returns FALSE if the error failed to send.
 * 
**/
bool IRSmartHubFirebaseFunctions::sendError(const int errCode) {
  // Ensure hubResult doesn't contain garbage
  initializeHubResult();

  // Set only resultCode and timestamp
  hubResult.resultCode = errCode;
  hubResult.timestamp = String(millis());

  FirebaseJson json;
  json.add("resultCode", hubResult.resultCode);
  json.add("timestamp", hubResult.timestamp);

  return sendToFirebase(ResultPath, json);
}

/**
 *	Attempts to send the FirebaseJson object to the designated path. 
 *  Returns TRUE if the action succeeded and FALSE if there was an error.
**/
#define FF_DEBUG
bool IRSmartHubFirebaseFunctions::sendToFirebase(const String& path, FirebaseJson& firebaseJson)
{
    if (Firebase.set(firebaseDataSEND, path, firebaseJson)) {
        #ifdef FF_DEBUG
            Serial.println("------------------------------------");
            Serial.println("Successfully sent data!");
            Serial.println("PATH: " + firebaseDataSEND.dataPath());
            Serial.println("TYPE: " + firebaseDataSEND.dataType());
            Serial.println("------------------------------------");
            Serial.println();
        #endif

        return true;
    } else {
        #ifdef FF_DEBUG
            Serial.println("------------------------------------");
            Serial.println("Failed to send data...");
            Serial.println("REASON: " + firebaseDataSEND.errorReason());
            Serial.println("------------------------------------");
            Serial.println();
        #endif

        return false;
    }
}

/**
  *	Sets all the values of HubAction to initial values.
 **/
void IRSmartHubFirebaseFunctions::initializeHubAction()
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
void IRSmartHubFirebaseFunctions::initializeHubResult()
{
	hubResult.code = "_none_";
	hubResult.encoding = 0;
	hubResult.rawData = "_none_";
	hubResult.rawLen = 0;
	hubResult.timestamp = "_none_";
	hubResult.resultCode = 0;
	hubResult.repeat = false;
}

/** -------- IR-Related Functions -------- **/

#ifdef IR_FUNCTIONS_ENABLED
/**
 *
 **/
#define SND_REC_SIG_DEBUG
bool IRSmartHubFirebaseFunctions::sendRecordedSignal(const decode_results& results)
{
  // Ensure HubResult doesn't contain garbage
	initializeHubResult();

	hubResult.resultCode = RES_SEND_SIG;
	hubResult.encoding = results.decode_type; //typeToString(results.decode_type, results.repeat);
	hubResult.code = "0x" + resultToHexidecimal(results);
	hubResult.timestamp = String(millis());
	//hubResult.rawData = rawDataToString(results);
	hubResult.rawLen = getCorrectedRawLength(results);

  if (!sendResult()) {
    #ifdef SND_REC_SIG_DEBUG
    Serial.println();
    Serial.println("Failed to send signal result... skipping rawData send!");
    #endif

    return false;
  }

	return sendRawData(results);
}

/**
 *	Sends raw data in chunks of 50 words at a time. The first thing sent is
 *	the number of chunks, followed by each chunk with its position in the
 *	array.
**/
#define RAW_DATA_DEBUG
bool IRSmartHubFirebaseFunctions::sendRawData(const decode_results& results) {
  String path = BasePath + "/rawData";
  int numChunks = getCorrectedChunkCount(hubResult.rawLen);

  #ifdef RAW_DATA_DEBUG
    Serial.println();
  #endif

	for (int i = 0; i < numChunks; i++) {
		String rawDataStr = rawDataToString(results.rawbuf, results.rawlen, (i * CHUNK_SIZE) + 1, true);

  #ifdef RAW_DATA_DEBUG
		Serial.print("Sending: ");
		Serial.println(rawDataStr);
  #endif
    if (!Firebase.setString(firebaseDataSEND, path + "/" + i, rawDataStr)) {
      #ifdef RAW_DATA_DEBUG
      Serial.println();
      Serial.print("Failed to upload rawData (chunk ");
      Serial.print(i+1);
      Serial.print("/");
      Serial.println(numChunks);
      Serial.println("). Skipping the rest of the rawData...");
      #endif

      return false;
    }
	}

  #ifdef RAW_DATA_DEBUG
    Serial.println("Done!");
  #endif

  return true;
}

/**
 *	Return the corrected length of a 'raw' format array structure after over-large values are
 *	converted into multiple entries. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
**/
uint16_t IRSmartHubFirebaseFunctions::getCorrectedRawLength(const decode_results& results) 
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

String IRSmartHubFirebaseFunctions::resultToHexidecimal(const decode_results& result) {
  String output = "";
  output += uint64ToString(result.value, 16);

  return output;
}

/**
  *	Converts the raw data from array of uint16_t to a string.
  * Note: Trying to convert more than CHUNK_SIZE could lead to
  *	memory instability.
 **/
String IRSmartHubFirebaseFunctions::rawDataToString(volatile uint16_t* rawbuf, uint16_t rawLen, uint16_t startPos, bool limitToChunk)
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

String IRSmartHubFirebaseFunctions::uint64ToString(uint64_t input, uint8_t base)
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
#endif


/** -------- DEBUG FUNCTION -------- **/
#if defined(HAR_DEBUG)
void printResult(StreamData &data)
{

  if (data.dataType() == "int")
    Serial.println(data.intData());
  else if (data.dataType() == "float")
    Serial.println(data.floatData(), 5);
  else if (data.dataType() == "double")
    printf("%.9lf\n", data.doubleData());
  else if (data.dataType() == "boolean")
    Serial.println(data.boolData() == 1 ? "true" : "false");
  else if (data.dataType() == "string" || data.dataType() == "null")
    Serial.println(data.stringData());
  else if (data.dataType() == "json")
  {
    Serial.println();
    FirebaseJson *json = data.jsonObjectPtr();
    //Print all object data
    Serial.println("Pretty printed JSON data:");
    String jsonStr;
    json->toString(jsonStr, true);
    Serial.println(jsonStr);
    Serial.println();
    Serial.println("Iterate JSON data:");
    Serial.println();
    size_t len = json->iteratorBegin();
    String key, value = "";
    int type = 0;
    for (size_t i = 0; i < len; i++)
    {
      json->iteratorGet(i, type, key, value);
      Serial.print(i);
      Serial.print(", ");
      Serial.print("Type: ");
      Serial.print(type == FirebaseJson::JSON_OBJECT ? "object" : "array");
      if (type == FirebaseJson::JSON_OBJECT)
      {
        Serial.print(", Key: ");
        Serial.print(key);
      }
      Serial.print(", Value: ");
      Serial.println(value);
    }
    json->iteratorEnd();
  }
  else if (data.dataType() == "array")
  {
    Serial.println();
    //get array data from FirebaseData using FirebaseJsonArray object
    FirebaseJsonArray *arr = data.jsonArrayPtr();
    //Print all array values
    Serial.println("Pretty printed Array:");
    String arrStr;
    arr->toString(arrStr, true);
    Serial.println(arrStr);
    Serial.println();
    Serial.println("Iterate array values:");
    Serial.println();

    for (size_t i = 0; i < arr->size(); i++)
    {
      Serial.print(i);
      Serial.print(", Value: ");

      FirebaseJsonData *jsonData = data.jsonDataPtr();
      //Get the result data from FirebaseJsonArray object
      arr->get(*jsonData, i);
      if (jsonData->typeNum == FirebaseJson::JSON_BOOL)
        Serial.println(jsonData->boolValue ? "true" : "false");
      else if (jsonData->typeNum == FirebaseJson::JSON_INT)
        Serial.println(jsonData->intValue);
      else if (jsonData->typeNum == FirebaseJson::JSON_FLOAT)
        Serial.println(jsonData->floatValue);
      else if (jsonData->typeNum == FirebaseJson::JSON_DOUBLE)
        printf("%.9lf\n", jsonData->doubleValue);
      else if (jsonData->typeNum == FirebaseJson::JSON_STRING ||
               jsonData->typeNum == FirebaseJson::JSON_NULL ||
               jsonData->typeNum == FirebaseJson::JSON_OBJECT ||
               jsonData->typeNum == FirebaseJson::JSON_ARRAY)
        Serial.println(jsonData->stringValue);
    }
  }
  else if (data.dataType() == "blob")
  {

    Serial.println();

    for (int i = 0; i < data.blobData().size(); i++)
    {
      if (i > 0 && i % 16 == 0)
        Serial.println();

      if (i < 16)
        Serial.print("0");

      Serial.print(data.blobData()[i], HEX);
      Serial.print(" ");
    }
    Serial.println();
  }
  else if (data.dataType() == "file")
  {

    Serial.println();

    File file = data.fileStream();
    int i = 0;

    while (file.available())
    {
      if (i > 0 && i % 16 == 0)
        Serial.println();

      int v = file.read();

      if (v < 16)
        Serial.print("0");

      Serial.print(v, HEX);
      Serial.print(" ");
      i++;
    }
    Serial.println();
    file.close();
  }
}
#endif