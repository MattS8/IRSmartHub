#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#define OUT

#define IR_DEBUG_IR_FUNC 1
//#define IR_DEBUG 1
#define AFF_DEBUG 1
//#define AFF_DEBUG_PARSE 1
//#define IRSMARTHUB_UNIT_TESTS 1

#ifdef IRSMARTHUB_UNIT_TESTS
#include "UnitTests.h"
#endif

#include "ArduinoFirebaseFunctionsESP8266.h"
//#include <DNSServer.h>            				// Local DNS Server used for redirecting all requests to the configuration portal
//#include <ESP8266WebServer.h>     				// Local WebServer used to serve the configuration portal
//#include "WiFiManager.h"          				// https://github.com/tzapu/WiFiManager WiFi Configuration Magic
//#include <ESP8266WiFi.h>
// Smart IR Modules
#ifdef IR_DEBUG
#include "IRSmartHubDebug.h"						// Debugging functions
#endif
#include "ArduinoIRFunctions.h"					// Contains all functionality having to do with controlling the IR sender/receiver

#define ON LOW
#define OFF HIGH

const String AP_NAME_BASE = "IRSmartHub-";
const uint32_t SMART_HUB_BAUD_RATE = 115200;

/* ------------------------- IR Hub States ------------------------- */

#define STATE_CONFIG_WIFI 1
#define STATE_CONFIG_FIREBASE 2

#endif