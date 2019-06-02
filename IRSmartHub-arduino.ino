#include "IRSmartHub-arduino.h"

#define ON LOW
#define OFF HIGH
#define AP_NAME_BASE "IRSmartHub-"

/* -------------------- IR Hub States -------------------- */

#define STATE_CONFIG_WIFI 1
#define STATE_CONFIG_FIREBASE 2

/* -------------------- DEBUG VALUES -------------------- */

const String dWifiManager = "WiFiManager: ";
#define bDEBUG true

/* -------------------- Global Variables -------------------- */
int ir_hub_state = STATE_CONFIG_WIFI;
ArduinoIRFunctions IRFunctions;
ArduinoFirebaseFunctions FirebaseFunctions;

String WifiAPName;

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

void debug_pulse_LED()
{
	digitalWrite(LED_BUILTIN, ON);
	delay(500);
	digitalWrite(LED_BUILTIN, OFF);
	delay(500);
	digitalWrite(LED_BUILTIN, ON);
	delay(500);	
	digitalWrite(LED_BUILTIN, OFF);	
}

void debug_printResults(decode_results* results)
{
   Serial.println(resultToHumanReadableBasic(results));
   // Output RAW timing info of the result.
   Serial.println(resultToTimingInfo(results));
   yield();  // Feed the WDT (again)

   // Output the results as source code
   Serial.println(resultToSourceCode(results));
   Serial.println("");  // Blank line between entries
   yield();             // Feed the WDT (again)
}

/* -------------------- Wifi Manager Callbacks -------------------- */

void onSaveConfig() 
{
	if (bDEBUG) debug_printOnSaveConfig();
	ir_hub_state = STATE_CONFIG_FIREBASE;
	FirebaseFunctions.connect();
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
	if (!wifiManager.autoConnect(WifiAPName.c_str()))
	{
		if (bDEBUG) Serial.println("Couldn't connect.");
		wifiManager.startConfigPortal(WifiAPName.c_str());
	} else {
		if (bDEBUG) Serial.println("Connected!");
		FirebaseFunctions.connect();
	}
}

/* -------------------- Arduino Firebase Functions -------------------- */

void ArduinoFirebaseFunctions::connect()
{
	if (bDEBUG) Serial.println("Connecting to firebase...");
	Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
	if (bDEBUG) Serial.println(ActionPath.c_str());
	Firebase.stream(ActionPath);
}

void ArduinoFirebaseFunctions::setHubName(const String& name)
{
	Serial.println("TODO - setHubName");
}

void ArduinoFirebaseFunctions::sendRecordedSignal(decode_results* results)
{
	Serial.println("TODO - sendRecordedSignal");
}

void ArduinoFirebaseFunctions::sendError(const int errorType)
{
	Serial.println("TODO - sendError");
	switch (errorType)
	{

	}
}

/* -------------------- Arduino IR Functions -------------------- */

void ArduinoIRFunctions::readNextSignal() //TODO
{
	// Variable to store results
	decode_results results;

	// Start listening for IR signals
	irReceiver.enableIRIn();

	if (bDEBUG) Serial.print("Listeneing for IR Signal");
	long dDelayTimer = millis();
	
	// Get current time for checks against timeout 
	long timeoutTimer = millis();

	// Continue to loop until a valid signal is read or timeout
	while (true)
	{		
		// Check for timeout
		if (millis() - timeoutTimer >= IR_READ_TIMEOUT)
		{
			if (bDEBUG) Serial.println("readNextSignal timeout!");
			FirebaseFunctions.sendError(ERR_TIMEOUT);
			break;
		}

		// Debug statment prints "." every second until IR signal has been read
		if (bDEBUG && millis() - dDelayTimer >= 1000) { dDelayTimer = millis(); Serial.print("."); }

		// Debug to check timeout functionality

		// Check if complete IR signal has been read
		if (!irReceiver.decode(&results)){
			delay(IR_RECV_MESSAGE_TIMEOUT);
			continue;
		}

		if (bDEBUG) Serial.println("Got results!");

		// Check for overflow
		if (results.overflow) 
		{
			if (bDEBUG) Serial.println("Overflow occurred...");
			FirebaseFunctions.sendError(ERR_OVERFLOW);
			break;
		}

		// Debug statement prints signal results
		if (bDEBUG) debug_printResults(&results);

		if (!results.repeat)
		{
			if (bDEBUG) Serial.println("Sending...");
			FirebaseFunctions.sendRecordedSignal(&results);			
			break;
		} 
		else if (bDEBUG) Serial.println("\nIgnoring repeat code...");
	}

	if (bDEBUG) Serial.println("Exiting loop");

	irReceiver.disableIRIn();
}

void ArduinoIRFunctions::sendSignal(const String& irSignal, bool bRepeat)
{
	Serial.println("TODO - sendSignal");
}

/* -------------------- Arduino Functions -------------------- */

void setup() 
{
	Serial.begin(SMART_HUB_BAUD_RATE);

	// Setup dynamic string variables
	char* temp = (char*) malloc(50 * sizeof(char));

	// Set base path
	sprintf(temp, "/device/%lu", ESP.getChipId());
	FirebaseFunctions.BasePath = String(temp);

	// Set action path
	sprintf(temp, "%s/action", FirebaseFunctions.BasePath.c_str());
	FirebaseFunctions.ActionPath = String(temp);

	// Set result path
	sprintf(temp, "%s/result", FirebaseFunctions.BasePath.c_str());
	FirebaseFunctions.ResultPath = String(temp);

	// Set Wifi Access Point Name
	sprintf(temp, "%s%lu", AP_NAME_BASE, ESP.getChipId());
	WifiAPName = String(temp);

	delete[] temp;

	pinMode(LED_BUILTIN, OUTPUT);
	connectToWifi();
	digitalWrite(LED_BUILTIN, OFF);
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
					IRFunctions.sendSignal(event.getString("/data/signal"),
										   event.getBool("/data/repeat"));
					break;
				case IR_ACTION_LEARN:
					if (bDEBUG) Serial.println("ir_action_learn");
					IRFunctions.readNextSignal();
					break;
				default: 
					if (bDEBUG) Serial.println("ERROR - Unknown action");
					break;
			}

			if (bDEBUG) debug_pulse_LED();

		}
	}
}