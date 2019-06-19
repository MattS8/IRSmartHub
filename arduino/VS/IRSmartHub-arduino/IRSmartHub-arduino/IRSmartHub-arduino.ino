/*
 Name:		IRSmartHub_arduino.ino
 Created:	6/18/2019 6:19:18 PM
 Author:	matth
*/

#include "IRSmartHub-arduino.h"

/* ------------------------ Global Variables ----------------------- */
int ir_hub_state = STATE_CONFIG_WIFI;
ArduinoIRFunctions IRFunctions;
ArduinoFirebaseFunctions FirebaseFunctions;
WiFiManager wifiManager;
String WifiAPName;

#ifdef IR_DEBUG
IRSmartHubDebug SHDebug;
#endif

/* --------------------- Wifi Manager Callbacks -------------------- */

void onSaveConfig()
{
#ifdef IR_DEBUG
	SHDebug.printOnSaveConfig();
	ir_hub_state = STATE_CONFIG_FIREBASE;
#endif

	// Set path to initial setup message
	char* temp = (char*)malloc(75 * sizeof(char));
	sprintf(temp, "/setups/%s/%lu", wifiManager.getConfigurer().c_str(),
		ESP.getChipId());
	FirebaseFunctions.SetupPath = String(temp);
	delete[] temp;

	// Connect to firebase
	FirebaseFunctions.connect();
}

void configModeCallback(WiFiManager* myWiFiManager)
{
#ifdef IR_DEBUG
	SHDebug.printConfigModeCallback(myWiFiManager);
#endif
}

/* -------------------- Wifi Manager Functions --------------------- */

void connectToWifi()
{
	wifiManager.setBreakAfterConfig(true);
	wifiManager.setDebugOutput(true);
	wifiManager.setAPCallback(configModeCallback);
	wifiManager.setSaveConfigCallback(onSaveConfig);

#ifdef IR_DEBUG 
	SHDebug.printStartingAutoConnect();
#endif
	if (!wifiManager.autoConnect(WifiAPName.c_str()))
	{
#ifdef IR_DEBUG 
		Serial.println("Couldn't connect.");
#endif
		wifiManager.startConfigPortal(WifiAPName.c_str());
	}
	else {
#ifdef IR_DEBUG 
		Serial.println("Automatically Connected!");
#endif
		FirebaseFunctions.connect();

	}
}

/* ----------------------- Arduino Functions ----------------------- */

void setup()
{
	Serial.begin(SMART_HUB_BAUD_RATE);

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

	// Enable debug statements
	FirebaseFunctions.setDebug(true);

	// Initialize IR hardware
	IRFunctions.init();
	SHDebug.init(false);

	pinMode(LED_BUILTIN, OUTPUT);
	connectToWifi();
	digitalWrite(LED_BUILTIN, OFF);
}

void loop()
{
#ifdef IR_DEBUG
	if (Firebase.failed()) {
		Serial.print("streaming error: ");
		Serial.print(FirebaseFunctions.SetupPath);
		Serial.println(Firebase.error());
		delay(1000);
		FirebaseFunctions.connect();
	}
#endif

	if (Firebase.available())
	{
		FirebaseObject event = Firebase.readEvent();
		String type = event.getString("type");
		type.toLowerCase();
		if (type == "put")
		{
#ifdef IR_DEBUG
			SHDebug.printFirebaseObject(event);
#endif

			switch (event.getInt("/data/type"))
			{
			case IR_ACTION_NONE:
#ifdef IR_DEBUG
				Serial.println("ir_action_none");
#endif
				break;
			case IR_ACTION_SEND:
#ifdef IR_DEBUG
				SHDebug.printSendAction(event.getString("/data/rawData"));
#endif
				IRFunctions.sendSignal(event.getString("/data/rawData"),
					(uint16_t)event.getInt("/data/rawLen"),
					event.getBool("/data/repeat"));
				break;
			case IR_ACTION_LEARN:
#ifdef IR_DEBUG
				Serial.println("ir_action_learn");
#endif
				IRFunctions.readNextSignal();
				break;
			default:
#ifdef IR_DEBUG
				Serial.println("ERROR - Unknown action");
#endif
				break;
			}

			SHDebug.pulse_LED();
		}
	}
}
