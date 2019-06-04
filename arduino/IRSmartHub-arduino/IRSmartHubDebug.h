#ifndef IRSMART_HUB_DEBUG_H
#define IRSMART_HUB_DEBUG_H

#include "WiFiManager.h"          		// https://github.com/tzapu/WiFiManager WiFi Configuration Magic
#include "IRrecv.h"						// Used to print IR data from decode_results
#include <ESP8266WiFi.h>          		// ESP8266 Core WiFi Library
#include "FirebaseArduino.h"			// https://github.com/FirebaseExtended/firebase-arduino

class IRSmartHubDebug {
public:
	void printFirebaseObject(FirebaseObject event);
	void printSendAction(const String& irSignal);
	void printOnSaveConfig();
	void printConfigModeCallback(WiFiManager* myWiFiManager);
	void printStartingAutoConnect();
	void pulse_LED();
	void printResults(decode_results* results);

	void sendTestIRSignal();

	void init(bool pulseLED);


private:
	bool bPulseLED = false;
	const String dWifiManager = "WiFiManager: ";
};

#endif