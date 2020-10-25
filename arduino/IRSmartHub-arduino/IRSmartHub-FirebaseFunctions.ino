#include "IRSmartHub-FirebaseFunctions.h"
// Copyright 2019 Matthew Steinhardt

/* --- Forward Declarations --- */
void handleActionReceived(StreamData data);
void handleTimeout(bool timeout);

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

    FirebaseJson json;
    json.add("sender", hubAction.sender);
    json.add("timestamp", hubAction.timestamp);
    json.add("type", hubAction.type);
    json.add("rawLen", hubAction.rawLen);
    json.add("reapeat", hubAction.repeat ? 1 : 0);

    bool success = sendToFirebase(ActionPath, json);
	delay(300);

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
                #endif
            }
        }

    } else {
        #ifdef HAR_DEBUG
            Serial.print("Stream returned non-JSON response: ");
            Serial.println(data.dataType());
        #endif

        return;
    }
}

/**
 *	Attempts to send the FirebaseJson object to the designated path. 
 *  Returns TRUE if the action succeeded and FALSE if there was an error.
**/
#define FF_DEBUG
bool IRSmartHubFirebaseFunctions::sendToFirebase(const String& path, FirebaseJson firebaseJson)
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


/** -------- DEBUG FUNCTION -------- **/

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