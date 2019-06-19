#include "IRSmartHub-arduino.h"

/* ------------------------ Global Variables ----------------------- */
int ir_hub_state = STATE_CONFIG_WIFI;
ArduinoIRFunctions IRFunctions;
ArduinoFirebaseFunctions FirebaseFunctions;
//WiFiManager wifiManager;
String WifiAPName;

#ifdef IR_DEBUG
IRSmartHubDebug SHDebug;
#endif

/* --------------------- Wifi Manager Callbacks -------------------- */

//void onSaveConfig() 
//{
//	#ifdef IR_DEBUG
//	SHDebug.printOnSaveConfig();
//	ir_hub_state = STATE_CONFIG_FIREBASE;
//	#endif
//
//	// Set path to initial setup message
//	char* temp = (char*) malloc(75 * sizeof(char));
//	sprintf(temp, "/setups/%s/%lu", wifiManager.getConfigurer().c_str(), 
// 		ESP.getChipId());
// 	FirebaseFunctions.SetupPath = String(temp);
//	delete[] temp;
//
//	// Connect to firebase
//	FirebaseFunctions.connect();
//}
//
//void configModeCallback (WiFiManager *myWiFiManager) 
//{
//	#ifdef IR_DEBUG
//	SHDebug.printConfigModeCallback(myWiFiManager);
//	#endif
//}

/* -------------------- Wifi Manager Functions --------------------- */

//void connectToWifi() 
//{
//	wifiManager.setBreakAfterConfig(true);
//	wifiManager.setDebugOutput(true);
//	wifiManager.setAPCallback(configModeCallback);
//	wifiManager.setSaveConfigCallback(onSaveConfig);
//
//	#ifdef IR_DEBUG 
//	SHDebug.printStartingAutoConnect(); 
//	#endif
//	if (!wifiManager.autoConnect(WifiAPName.c_str()))
//	{
//		#ifdef IR_DEBUG 
//		Serial.println("Couldn't connect.");
//		#endif
//		wifiManager.startConfigPortal(WifiAPName.c_str());
//	} else {
//		#ifdef IR_DEBUG 
//		Serial.println("Automatically Connected!");
//		#endif
//		//FirebaseFunctions.connect();
//	}
//}

/* ----------------------- Arduino Functions ----------------------- */

void setup() 
{
	Serial.begin(SMART_HUB_BAUD_RATE);

	// Setup dynamic string variables
	char* temp = (char*) malloc(50 * sizeof(char));

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

#ifdef IR_DEBUG
	Serial.println("");
	Serial.print("BasePath = "); Serial.println(FirebaseFunctions.BasePath);
	Serial.print("ActionPath = "); Serial.println(FirebaseFunctions.ActionPath);
	Serial.print("ResultPath = "); Serial.println(FirebaseFunctions.ResultPath);
#endif

	// Initialize IR hardware
	IRFunctions.init();
	SHDebug.init(false);

	pinMode(LED_BUILTIN, OUTPUT);
	//connectToWifi();

	WiFi.begin("HawkswayBase", "F4d29095dc");
#ifdef IR_DEBUG
	Serial.print("Connecting to Wi-Fi");
#endif // IR_DEBUG
	while (WiFi.status() != WL_CONNECTED)
	{
#ifdef IR_DEBUG
		Serial.print(".");
#endif // IR_DEBUG
		delay(300);
	}
#ifdef IR_DEBUG
	Serial.println();
	Serial.print("Connected with IP: ");
	Serial.println(WiFi.localIP());
	Serial.println();
#endif // IR_DEBUG
	digitalWrite(LED_BUILTIN, OFF);

	FirebaseFunctions.connect();
}

void loop()
{

	/* NEW FIREBASE IMPLEMENTATION  */
	FirebaseFunctions.readStreamData();

	FirebaseFunctions.streamTimeout();

	if (FirebaseFunctions.receivedHubAction())
	{
#ifdef IR_DEBUG
		Serial.println(DEBUG_DIV);
		Serial.println(F("Stream Data Available..."));
		Serial.println("STREAM PATH: " + firebaseReadData.streamPath());
		Serial.println("EVENT PATH: " + firebaseReadData.dataPath());
		Serial.println("DATA TYPE: " + firebaseReadData.dataType());
		Serial.println("EVENT TYPE: " + firebaseReadData.eventType());
		Serial.println(F("HUB ACTION: "));
		Serial.print("    - Type: "); Serial.print(SHDebug.getActionString(hubAction.type)); Serial.println("");
		if (hubAction.type == IR_ACTION_SEND)
		{
			Serial.print("    - rawData: "); Serial.print(hubAction.rawData); Serial.println("");
			Serial.print("    - rawLen: "); Serial.print(hubAction.rawLen); Serial.println("");
		}

		Serial.println(DEBUG_DIV);
#endif //IR_DEBUG
	}

}