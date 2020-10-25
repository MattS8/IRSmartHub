#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#define OUT

#ifdef IRSMARTHUB_UNIT_TESTS
#include "UnitTests.h"
#endif


#include "FirebaseESP8266.h"
#include <ESP8266WiFi.h>

//#include <DNSServer.h>            				// Local DNS Server used for redirecting all requests to the configuration portal
//#include <ESP8266WebServer.h>     				// Local WebServer used to serve the configuration portal
//#include "WiFiManager.h"          				// https://github.com/tzapu/WiFiManager WiFi Configuration Magic


/* ------------------------- Firebase ------------------------- */
#include "IRSmartHub-FirebaseFunctions.h"


/* ------------------------ IR Functions ----------------------- */
// Smart IR Modules
#ifdef IR_DEBUG
#include "IRSmartHubDebug.h"						// Debugging functions
#endif
//#include "ArduinoIRFunctions.h"					// Contains all functionality having to do with controlling the IR sender/receiver

#define ON LOW
#define OFF HIGH

const String AP_NAME_BASE = "IRSmartHub-";
const uint32_t SMART_HUB_BAUD_RATE = 115200;

#endif