#include "IRSmartHubDebug.h"

void IRSmartHubDebug::printFirebaseObject(FirebaseObject event)
{
	Serial.print("path: ");
	Serial.println(event.getString("path"));
	Serial.print("data: ");
	event.getJsonVariant("data").prettyPrintTo(Serial);
	Serial.println("");
	Serial.print("Performing action: ");
}

void IRSmartHubDebug::printSendAction(const String& irSignal)
{
	Serial.print("ir_action_send (");
	Serial.print(irSignal);
	Serial.println(")");
}

void IRSmartHubDebug::printOnSaveConfig()
{
	Serial.print(dWifiManager);
	Serial.println("Saving SSID and password info...");
}

void IRSmartHubDebug::printConfigModeCallback(WiFiManager* myWiFiManager)
{
	Serial.print(dWifiManager);
	Serial.println("Entered config mode");
	Serial.println(WiFi.softAPIP());
	Serial.println("-----");
	Serial.println(myWiFiManager->getConfigPortalSSID());
}

void IRSmartHubDebug::printStartingAutoConnect()
{
	Serial.print(dWifiManager);
	Serial.println("Starting autoConnect...");
}

void IRSmartHubDebug::pulse_LED()
{
	digitalWrite(LED_BUILTIN, ON);
	delay(500);
	digitalWrite(LED_BUILTIN, OFF);
	delay(500);
	digitalWrite(LED_BUILTIN, ON);
	delay(500);	
	digitalWrite(LED_BUILTIN, OFF);
}

void IRSmartHubDebug::printResults(decode_results* results)
{
   Serial.println(resultToHumanReadableBasic(results));

   // Output RAW timing info of the result.
   Serial.println(resultToTimingInfo(results));
   yield();

   // Output the results as source code
   String resSourceCode = resultToSourceCode(results);
   Serial.println(resSourceCode);
   yield();
}


