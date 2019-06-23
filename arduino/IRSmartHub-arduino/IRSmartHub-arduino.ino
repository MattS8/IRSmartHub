#include "IRSmartHub-arduino.h"

/* ------------------------ Global Variables ----------------------- */
int ir_hub_state = STATE_CONFIG_WIFI;

#ifdef ARDUINO_IR_FUNCTIONS_H
ArduinoIRFunctions IRFunctions;
#endif 

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
ArduinoFirebaseFunctions FirebaseFunctions;
#endif
//WiFiManager wifiManager;
String WifiAPName;

#ifdef IRSMARTHUB_UNIT_TESTS
bool bRanTests = false;
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

void connectToWifi() 
{
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

	//TEMP:	TODO - Replace with wifi manager solution
	WiFi.begin("HawkswayBase", "F4d29095dc");
#ifdef IR_DEBUG
	Serial.print("Connecting to Wi-Fi");
#endif // IR_DEBUG
	long timeoutStart = millis();
	while (WiFi.status() != WL_CONNECTED)
	{
#ifdef IR_DEBUG
		Serial.print(".");
#endif // IR_DEBUG
		delay(300);
		if (millis() - timeoutStart > 8000)
			ESP.restart();
	}
#ifdef IR_DEBUG
	Serial.println();
	Serial.print("Connected with IP: ");
	Serial.println(WiFi.localIP());
	Serial.println();
#endif // IR_DEBUG
	//TEMP:
}

/* ----------------------- Arduino Functions ----------------------- */

void setup() 
{
	Serial.begin(SMART_HUB_BAUD_RATE);
#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
	// Setup dynamic string variables
	char* temp = (char*)malloc(50 * sizeof(char));

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

#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H

	// Initialize IR hardware
#ifdef ARDUINO_IR_FUNCTIONS_H
	IRFunctions.init();
#endif // ARDUINO_IR_FUNCTIONS_H

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
	// Initialize FirebaseFunctions
	FirebaseFunctions.setup();
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H

	// Set pin mode for onboard LED
	pinMode(LED_BUILTIN, OUTPUT);

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
	// Start wifi connection process
	connectToWifi();
	
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H

	// Turn onboard LED off
	digitalWrite(LED_BUILTIN, OFF);

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
	// Start connection to firebase process
	FirebaseFunctions.connect();
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
}

void loop()
{

#ifdef IRSMARTHUB_UNIT_TESTS
	if (!bRanTests)
	{
		UTests.testAll();
		bRanTests = true;
	}

	return;
#endif // IRSMARTHUB_UNIT_TESTS

	/* NEW FIREBASE IMPLEMENTATION  */
#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
#ifdef ARDUINO_IR_FUNCTIONS_H
	// React to new action, if received
	if (FirebaseFunctions.receivedHubAction())
	{
#ifdef IR_DEBUG
		Serial.print("Action: ");
#endif // IR_DEBUG

		switch (hubAction.type)
		{
		case IR_ACTION_LEARN:
#ifdef IR_DEBUG
			Serial.println("LEARN");
#endif //IT_DEBUG
			IRFunctions.readNextSignal();
			break;
		case IR_ACTION_SEND:
#ifdef IR_DEBUG
			Serial.println("SEND");
#endif //IT_DEBUG
			IRFunctions.sendSignal(hubAction.rawData, hubAction.rawLen, hubAction.repeat);
			break;
		case IR_ACTION_NONE:
#ifdef IR_DEBUG
			Serial.println("NONE");
#endif //IT_DEBUG
			break;
		default:
#ifdef IR_DEBUG
			Serial.print("UNKNOWN - "); Serial.println(hubAction.sender);
#endif //IT_DEBUG
			break;
		}

		//// Added delay so readStreamData() isn't being instantly polled
		//delay(300);
	}
#endif // ARDUINO_IR_FUNCTIONS_H
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
}