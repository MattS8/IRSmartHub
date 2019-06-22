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

#ifdef AFF_DEBUG
#ifdef IR_DEBUG_IR_FUNC
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
#endif // IR_DEBUG_IR_FUNC
#endif // AFF_DEBUG



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

//void IRSmartHubDebug::printConfigModeCallback(WiFiManager* myWiFiManager)
//{
//	Serial.print(dWifiManager);
//	Serial.println("Entered config mode");
//	Serial.println(WiFi.softAPIP());
//	Serial.println("-----");
//	Serial.println(myWiFiManager->getConfigPortalSSID());
//}

//void IRSmartHubDebug::printStartingAutoConnect()
//{
//	Serial.print(dWifiManager);
//	Serial.println("Starting autoConnect...");
//}

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

#ifdef IR_DEBUG_IR_FUNC
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
#endif // IR_DEBUG_IR_FUNC



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


