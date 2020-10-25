#include "IRSmartHub-arduino.h"

/* ------------------------ Global Variables ----------------------- */
IRSmartHubFirebaseFunctions FirebaseFunctions;
String WifiAPName;

/* -------------------- Wifi Manager Functions --------------------- */

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
		if (millis() - timeoutStart > 8000){
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

#ifdef SETUP_DEBUG
	Serial.println("");
	Serial.print("BasePath = "); Serial.println(FirebaseFunctions.BasePath);
	Serial.print("ActionPath = "); Serial.println(FirebaseFunctions.ActionPath);
	Serial.print("ResultPath = "); Serial.println(FirebaseFunctions.ResultPath);
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
}