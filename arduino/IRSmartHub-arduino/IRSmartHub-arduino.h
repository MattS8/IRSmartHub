#ifndef IRSMART_HUB_ARDUINO
#define IRSMART_HUB_ARDUINO

#define IR_DEBUG 1

#include "FirebaseArduino.h"			// https://github.com/FirebaseExtended/firebase-arduino
#include <ESP8266WiFi.h>          		// ESP8266 Core WiFi Library
#include "ArduinoJson.h"				// https://github.com/bblanchon/ArduinoJson
#include <DNSServer.h>            		// Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     		// Local WebServer used to serve the configuration portal
#include "WiFiManager.h"          		// https://github.com/tzapu/WiFiManager WiFi Configuration Magic

// Smart IR Modules
#ifdef IR_DEBUG
#include "IRSmartHubDebug.h"			// Debugging functions
#endif
#include "ArduinoIRFunctions.h"			// Contains all functionality having to do with controlling the IR sender/receiver
#include "ArduinoFirebaseFunctions.h"	// Contains all functionality having to do with communicating with Firebase backend

const uint32_t SMART_HUB_BAUD_RATE = 115200;

#endif