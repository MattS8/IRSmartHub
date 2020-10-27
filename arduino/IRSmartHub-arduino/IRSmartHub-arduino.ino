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
	FirebaseFunctions.connect();
}

void loop()
{
#ifdef LOOP_DEBUG
	Serial.print(".");
#endif

	if (newHubActionReceieved)
		runAction();
}

#define ACTION_DEBUG
void runAction()
{
	// Consume new hub action
	newHubActionReceieved = false;

	switch (hubAction.type)
	{
	case IR_ACTION_LEARN:
#ifdef ACTION_DEBUG
		Serial.println();
		Serial.println("Listening for new IR signal...");
#endif
		learnSignal();
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
		//FirebaseFunctions.initializeHubResult();

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