#include "IRSmartHubDebug.h"

//void IRSmartHubDebug::printFirebaseObject(FirebaseObject event)
//{
//	Serial.print("path: ");
//	Serial.println(event.getString("path"));
//	Serial.print("data: ");
//	event.getJsonVariant("data").prettyPrintTo(Serial);
//	Serial.println("");
//	Serial.print("Performing action: ");
//}


void IRSmartHubDebug::printStreamData()
{
	Serial.println(F("Stream Data Available..."));
	Serial.println("STREAM PATH: " + firebaseReadData.streamPath());
	Serial.println("EVENT PATH: " + firebaseReadData.dataPath());
	Serial.println("DATA TYPE: " + firebaseReadData.dataType());
	Serial.println("EVENT TYPE: " + firebaseReadData.eventType());
	Serial.println(F("HUB ACTION: "));
	Serial.print("    - Type: "); Serial.print(SHDebug.getActionString(hubAction.type)); Serial.println("");
	if (hubAction.type == IR_ACTION_SEND)
	{
		Serial.print("    - rawData: "); Serial.print(hubAction.rawData); Serial.println("");
		Serial.print("    - rawLen: "); Serial.print(hubAction.rawLen); Serial.println("");
	}
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

void IRSmartHubDebug::printResults(decode_results* results)
{
	Serial.println("Human Readable Basic Info:");
   Serial.println(resultToHumanReadableBasic(results));

   // Output RAW timing info of the result.
   Serial.println("Timing Info:");
   Serial.println(resultToTimingInfo(results));
   yield();

   // Output the results as source code
   Serial.println("Source Code:");
   String resSourceCode = resultToSourceCode(results);
   Serial.println(resSourceCode);
   yield();
}

void IRSmartHubDebug::sendTestIRSignal()
{
	pinMode(12, OUTPUT);
	for (int i = 0; i < 200; i++) 
	{
		digitalWrite(12, HIGH);
		delay(300);
		digitalWrite(12, LOW);
		delay(300);
	}
}

String IRSmartHubDebug::getActionString(int type)
{
	switch (type)
	{ 
		case IR_ACTION_LEARN:  
			return "LEARN";
		case IR_ACTION_SEND:
			return "SEND";
		case IR_ACTION_NONE:
			return "NONE";
	}

	return "UKNOWN";
}

void IRSmartHubDebug::init(bool pulseLED)
{
	bPulseLED = pulseLED;
}

void IRSmartHubDebug::pulse_LED()
{
	if (!bPulseLED)
		return;

	digitalWrite(LED_BUILTIN, ON);
	delay(500);
	digitalWrite(LED_BUILTIN, OFF);
	delay(500);
	digitalWrite(LED_BUILTIN, ON);
	delay(500);	
	digitalWrite(LED_BUILTIN, OFF);
}


