#include "IRSmartHub-arduino.h"

#define ON LOW
#define OFF HIGH
#define AP_NAME "SMART-IR-DEBUG_001"

#define IR_BLAST_PIN 100

/* -------------------- IR Hub States -------------------- */

#define STATE_CONFIG_WIFI 1
#define STATE_CONFIG_FIREBASE 2

/* -------------------- DEBUG VALUES -------------------- */

String dWifiManager = "WiFiManager: ";
#define bDEBUG true

/* -------------------- Global Variables -------------------- */
int ir_hub_state = STATE_CONFIG_WIFI;
ArduinoIRFunctions IRFunctions;
ArduinoFirebaseFunctions FirebaseFunctions;

/* -------------------- Debug Functions -------------------- */

void debug_printFirebaseObject(FirebaseObject event)
{
	Serial.print("path: ");
	Serial.println(event.getString("path"));
	Serial.print("data: ");
	event.getJsonVariant("data").prettyPrintTo(Serial);
	Serial.print("Performing action: ");
}

void debug_printSendAction(const String& irSignal)
{
	Serial.print("ir_action_send (");
	Serial.print(irSignal);
	Serial.println(")");
}

void debug_printOnSaveConfig()
{
	Serial.print(dWifiManager);
	Serial.println("Saving SSID and password info...");
}

void debug_printConfigModeCallback(WiFiManager* myWiFiManager)
{
	Serial.print(dWifiManager);
	Serial.println("Entered config mode");
	Serial.println(WiFi.softAPIP());
	Serial.println("-----");
	Serial.println(myWiFiManager->getConfigPortalSSID());
}

void debug_printStartingAutoConnect()
{
	Serial.print(dWifiManager);
	Serial.println("Starting autoConnect...");
}

/* -------------------- Wifi Manager Callbacks -------------------- */

void onSaveConfig() 
{
	if (bDEBUG) debug_printOnSaveConfig();
	ir_hub_state = STATE_CONFIG_FIREBASE;
}

void configModeCallback (WiFiManager *myWiFiManager) 
{
	if (bDEBUG) debug_printConfigModeCallback(myWiFiManager);
}

/* -------------------- Wifi Manager Functions -------------------- */

void connectToWifi() 
{
	WiFiManager wifiManager;
	wifiManager.setBreakAfterConfig(true);
	wifiManager.setDebugOutput(true);
	wifiManager.setAPCallback(configModeCallback);
	wifiManager.setSaveConfigCallback(onSaveConfig);

	if (bDEBUG) debug_printStartingAutoConnect();
	if (!wifiManager.autoConnect(AP_NAME))
	{
		if (bDEBUG) Serial.println("Couldn't connect.");
		wifiManager.startConfigPortal(AP_NAME);
	} else {
		if (bDEBUG) Serial.println("Connected!");
		FirebaseFunctions.connect();
	}
}

/* -------------------- Arduino Firebase Functions -------------------- */

void ArduinoFirebaseFunctions::connect()
{
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
	Firebase.stream("/devices/" + IR_UID + "/action");
}

void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	Serial.println("TODO - setHubName");
}


/* -------------------- Arduino IR Functions -------------------- */

void ArduinoIRFunctions::readNextSignal() //TODO
{
	Serial.println("TODO - readNextSignal");
	lastReadSignal = "0xFFFFFFF";
}

void ArduinoIRFunctions::sendSignal(const String& irSignal)
{
	Serial.println("TODO - sendSignal");
}

/* -------------------- Arduino Functions -------------------- */

void setup() 
{
	Serial.begin(115200);
	pinMode(LED_BUILTIN, OUTPUT);
	connectToWifi();
	digitalWrite(LED_BUILTIN, ON);
}

void loop()
{
	if (Firebase.failed() && bDEBUG) {
		Serial.println("streaming error");
		Serial.println(Firebase.error());
	}

	if (Firebase.available())
	{
		FirebaseObject event = Firebase.readEvent();
		String type = event.getString("type");
		type.toLowerCase();
		if (type == "put") 
		{
			if (bDEBUG) debug_printFirebaseObject(event);

			switch (event.getInt("/data/type")) 
			{
				case IR_ACTION_NONE: 
					if (bDEBUG) Serial.println("ir_action_none");
					break;
				case IR_ACTION_SEND:
					if (bDEBUG) debug_printSendAction(event.getString("/data/signal"));
					IRFunctions.sendSignal(event.getString("/data/signal"));
					break;
				case IR_ACTION_LEARN:
					if (bDEBUG) Serial.println("ir_action_learn");
					IRFunctions.readNextSignal();
					break;
				default: 
					if (bDEBUG) Serial.println("ERROR - Unknown action");
					break;
			}

			digitalWrite(LED_BUILTIN, OFF);
			delay(500);
			digitalWrite(LED_BUILTIN, ON);
			delay(500);
			digitalWrite(LED_BUILTIN, OFF);
			delay(500);	
			digitalWrite(LED_BUILTIN, ON);
		}
	}
}