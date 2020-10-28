#include "IRSmartHub-arduino.h"

/* ------------------------ Global Variables ----------------------- */
IRSmartHubFirebaseFunctions FirebaseFunctions;
IRSmartHubIRFunctions IRFunctions;
String WifiAPName;

/* -------------------- Wifi Manager Functions --------------------- */

// Forward Declarations
void sendAllRawData();

#define CON_WIFI_DEBUG
void connectToWifi()
{
	//TODO - Replace with wifi manager solution
	WiFi.begin("HawkswayBase", "F4d29095dc");
#ifdef CON_WIFI_DEBUG
	Serial.print("Connecting to Wi-Fi");
#endif // CON_WIFI_DEBUG
	long timeoutStart = millis();
	while (WiFi.status() != WL_CONNECTED)
	{
#ifdef CON_WIFI_DEBUG
		Serial.print(".");
#endif // CON_WIFI_DEBUG
		delay(300);
		if (millis() - timeoutStart > 8000)
		{
#ifdef CON_WIFI_DEBUG
			Serial.println();
			Serial.println("Couldn't connect to wifi... timeout.");
			delay(1000);
#endif
			ESP.restart();
		}
	}
#ifdef CON_WIFI_DEBUG
	Serial.println();
	Serial.print("Connected with IP: ");
	Serial.println(WiFi.localIP());
	Serial.println();
#endif // CON_WIFI_DEBUG
}

/* ----------------------- Arduino Functions ----------------------- */

#define SETUP_DEBUG
void setup()
{
	Serial.begin(SMART_HUB_BAUD_RATE);

	// Setup dynamic string variables
	char *temp = (char *)malloc(50 * sizeof(char));

	// Set base path
	sprintf(temp, "/devices/%lu", ESP.getChipId());
	FirebaseFunctions.BasePath = String(temp);

	// Set action path
	sprintf(temp, "%s/action", FirebaseFunctions.BasePath.c_str());
	FirebaseFunctions.ActionPath = String(temp);

	// Set result path
	sprintf(temp, "%s/result", FirebaseFunctions.BasePath.c_str());
	FirebaseFunctions.ResultPath = String(temp);

	// Set Wifi Access Point Name
	sprintf(temp, "%s%lu", AP_NAME_BASE.c_str(), ESP.getChipId());
	WifiAPName = String(temp);

	delete[] temp;

#ifdef SETUP_DEBUG
	Serial.println("");
	Serial.print("BasePath = ");
	Serial.println(FirebaseFunctions.BasePath);
	Serial.print("ActionPath = ");
	Serial.println(FirebaseFunctions.ActionPath);
	Serial.print("ResultPath = ");
	Serial.println(FirebaseFunctions.ResultPath);
#endif // SETUP_DEBUG

	// 	// Initialize IR hardware
	// 	//IRFunctions.init();

	// Set pin mode for onboard LED
	pinMode(LED_BUILTIN, OUTPUT);

	// Start wifi connection process
	connectToWifi();

	// Turn onboard LED off
	digitalWrite(LED_BUILTIN, OFF);

// 	// Start connection to firebase process
#ifdef FIREBASE_FUNCTIONS_ENABLED
	FirebaseFunctions.connect();
#endif
}

void loop()
{
#ifdef LOOP_DEBUG
	Serial.print(".");
#endif

#ifdef FIREBASE_FUNCTIONS_ENABLED
	if (newHubActionReceieved)
		runAction();
#endif
}

#ifdef FIREBASE_FUNCTIONS_ENABLED
#define ACTION_DEBUG
void runAction()
{
	// Consume new hub action
	newHubActionReceieved = false;

	switch (hubAction.type)
	{
	case IR_ACTION_LEARN:
#ifdef IR_FUNCTIONS_ENABLED
		learnSignal();
#else
		Serial.println("\n--------\nWarning: IR Functionality Disabled. No signal will be learned!\n--------");
#endif
		// Clear hub action on backend to prevent repeated action
		FirebaseFunctions.initializeHubAction();
		FirebaseFunctions.sendAction();
		break;

	case IR_ACTION_SEND:
#ifdef IR_FUNCTIONS_ENABLED
		if (readRawData(IRFunctions.getCorrectedChunkCount(hubAction.rawLen)))
			IRFunctions.sendSignal(hubAction.rawData, hubAction.rawLen, hubAction.repeat);
		// Clear hub action on backend to prevent repeated action
		FirebaseFunctions.initializeHubAction();
		FirebaseFunctions.sendAction();
#else
		Serial.println("\n--------\nWarning: IR Functionality Disabled. Can't send IR signal to IR blaster!\n--------");
#endif
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
 *	Begins reading raw data chunks until all raw data has been
 *	received or a timeout occurs. On successful completetion,
 *	a RES_SEND_SUCC result is sent. On timeout, an ERR_TIMEOUT
 *	result is sent. 
 *	The resulting raw data is written into hubAction.rawData.
 * 
 *	Note: This function assumes hubAction has been initialized
 *	prior to calling.
**/
#define RRD_DEBUG
#ifdef IR_FUNCTIONS_ENABLED
bool readRawData(uint16_t numChunks)
{
	int chunksReceived = 0;
	long unsigned startTime = millis();
	String chunk;
	String path = FirebaseFunctions.BasePath + "/rawData/" + chunksReceived;
	uint16_t *marker;

#ifdef RRD_DEBUG
	if (hubAction.rawData != NULL)
	{
		Serial.println("WARNING: rawData was not NULL at start of readRawData()...");
	}
#endif

	// Allocate memory for rawData array
	hubAction.rawData = (uint16_t *)calloc(hubAction.rawLen, sizeof(uint16_t));
	marker = hubAction.rawData;

#ifdef RRD_DEBUG
	Serial.print("Reading in ");
	Serial.print(numChunks);
	Serial.println(" chunks...");
#endif

	while (chunksReceived < numChunks && millis() - startTime < READ_RAW_DATA_TIMEOUT)
	{
		// Read chunks
		if (Firebase.getString(firebaseDataRECV, path, chunk))
		{
			marker = IRFunctions.parseRawDataString(chunk.c_str(), hubAction.rawData, chunksReceived * CHUNK_SIZE);
			path = FirebaseFunctions.BasePath + "/rawData/" + ++chunksReceived;
		}
		else
		{
#ifdef RRD_DEBUG
			Serial.print("ERROR: Failed to read rawData string at ");
			Serial.println(chunksReceived);
#endif
		}
	}

#ifdef RRD_DEBUG
	for (int i = 0; i < numChunks; i++)
	{
		Serial.print("rawData[");
		Serial.print(i);
		Serial.print("]: ");
		Serial.println(IRFunctions.rawDataToString(hubAction.rawData, hubAction.rawLen, i * CHUNK_SIZE, true));
	}
#endif

	// Send err if chunksReceived != numChunks
	if (chunksReceived != numChunks)
	{
#ifdef RRD_DEBUG
		Serial.println("Didn't received all the chunks in alloted time.");
#endif
		FirebaseFunctions.sendError(ERR_TIMEOUT);
		return false;
	}
	else
	// Otherwise send success result
	{
		FirebaseFunctions.initializeHubResult();
		hubResult.resultCode = RES_SEND_SUCC;
		hubResult.timestamp = String(millis());
		if (!FirebaseFunctions.sendResult())
		{
#ifdef RRD_DEBUG
			Serial.println("ERROR: Failed to send succes result!");
#endif
		}

		if (!Firebase.setString(firebaseDataSEND, FirebaseFunctions.BasePath + "/rawData", "_none_"))
		{
#ifdef RRD_DEBUG
			Serial.println("ERROR: Failed to clear rawData endpoint!");
#endif
		}
#ifdef RRD_DEBUG
		Serial.println("Successfully read all raw data.");
#endif
		return true;
	}
}
#endif // IR_FUNCTIONS_ENABLED

/**
 * Tells the IR receiver to listen for an IR signal. Once a signal is read,
 * the result is sent to Firebase (at the hubResult location). If the receiver
 * times out or the signal overflows, the proper error is sent (also to the 
 * hubResult location) instead. 
 * 
**/
#define LRN_SIG_DEBUG
void learnSignal()
{
	IRFunctions.readNextSignal();
	switch (IRFunctions.readResult.resultCode)
	{
	case ERR_TIMEOUT:
		FirebaseFunctions.sendError(ERR_TIMEOUT);
		break;
	case ERR_OVERFLOW:
		FirebaseFunctions.sendError(ERR_OVERFLOW);
		break;
	case RES_SEND_SIG:
		// Ensure HubResult doesn't contain garbage
		FirebaseFunctions.initializeHubResult();

		hubResult.resultCode = RES_SEND_SIG;
		hubResult.encoding = IRFunctions.readResult.results.decode_type; //typeToString(results.decode_type, results.repeat);
		hubResult.code = "0x" + IRFunctions.resultToHexidecimal(IRFunctions.readResult.results);
		hubResult.timestamp = String(millis());
		//hubResult.rawData = rawDataToString(results);
		hubResult.rawLen = IRFunctions.getCorrectedRawLength(IRFunctions.readResult.results);

		if (FirebaseFunctions.sendResult())
			sendAllRawData();
		else
		{
#ifdef LRN_SIG_DEBUG
			Serial.println();
			Serial.println("Failed to send signal result... skipping rawData!");
#endif
		}
	default:
		break;
	}
}

/**
 * Sends all chunks of raw data to the proper endpoint(s) (/rawData/<index>).
**/
#define RAW_DATA_DEBUG
void sendAllRawData()
{
	int numChunks = IRFunctions.getCorrectedChunkCount(hubResult.rawLen);

#ifdef RAW_DATA_DEBUG
	Serial.println();
#endif

	for (int i = 0; i < numChunks; i++)
	{
		String rawDataStr = IRFunctions.rawDataToString(
			IRFunctions.readResult.results.rawbuf,
			IRFunctions.readResult.results.rawlen,
			(i * CHUNK_SIZE) + 1,
			true);

#ifdef RAW_DATA_DEBUG
		Serial.print("Sending: ");
		Serial.println(rawDataStr);
#endif
		if (!FirebaseFunctions.sendRawData(i, rawDataStr))
		{
#ifdef RAW_DATA_DEBUG
			Serial.println();
			Serial.print("Failed to upload rawData (chunk ");
			Serial.print(i + 1);
			Serial.print("/");
			Serial.println(numChunks);
			Serial.println("). Skipping the rest of the rawData...");
#endif
			return;
		}
	}

#ifdef RAW_DATA_DEBUG
	Serial.println("Done!");
#endif
}
#endif // FIREBASE_FUNCTIONS_ENABLED