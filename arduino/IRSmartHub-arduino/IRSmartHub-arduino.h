#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

/* 
    ------------------------------------------------------------------ 
    Enable / Disable modules by commenting/uncommenting these #defines 
    ------------------------------------------------------------------ 
*/
#define FIREBASE_FUNCTIONS_ENABLED                  // Enable Firebase functionality
#define IR_FUNCTIONS_ENABLED                        // Enable IR send/receive functionality

/* ------------------------------------------------------------------  */

// Generic #defines
#define OUT
#define ON LOW
#define OFF HIGH

#include <ESP8266WiFi.h>                            // Needed to connect to wifi

//#include <DNSServer.h>            				// Local DNS Server used for redirecting all requests to the configuration portal
//#include <ESP8266WebServer.h>     				// Local WebServer used to serve the configuration portal
//#include "WiFiManager.h"          				// https://github.com/tzapu/WiFiManager WiFi Configuration Magic


/* ------------------------- Firebase ------------------------- */
#ifdef FIREBASE_FUNCTIONS_ENABLED
#include "IRSmartHub-FirebaseFunctions.h"           // Contains all functionality having to do with communicating w/Firebase Realtime Database
#endif

/* ------------------------ IR Functions ----------------------- */
#ifdef IR_FUNCTIONS_ENABLED
#include "IRSmartHub-IRFunctions.h"					// Contains all functionality having to do with controlling the IR sender/receiver
#endif


// Constants
const String AP_NAME_BASE = "IRSmartHub-";
const uint32_t SMART_HUB_BAUD_RATE = 115200;

#endif