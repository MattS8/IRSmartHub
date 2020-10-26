#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#define FIREBASE_FUNCTIONS_ENABLED
#define IR_FUNCTIONS_ENABLED

#define OUT
#define ON LOW
#define OFF HIGH


#include "FirebaseESP8266.h"
#include <ESP8266WiFi.h>

//#include <DNSServer.h>            				// Local DNS Server used for redirecting all requests to the configuration portal
//#include <ESP8266WebServer.h>     				// Local WebServer used to serve the configuration portal
//#include "WiFiManager.h"          				// https://github.com/tzapu/WiFiManager WiFi Configuration Magic


/* ------------------------- Firebase ------------------------- */
#include "IRSmartHub-FirebaseFunctions.h"           // Contains all functionality having to do with communicating w/Firebase Realtime Database

/* ------------------------ IR Functions ----------------------- */
#include "IRSmartHub-IRFunctions.h"					// Contains all functionality having to do with controlling the IR sender/receiver


// Constants
const String AP_NAME_BASE = "IRSmartHub-";
const uint32_t SMART_HUB_BAUD_RATE = 74880;

#endif