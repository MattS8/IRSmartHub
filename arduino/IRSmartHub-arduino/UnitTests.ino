#ifdef IRSMARTHUB_UNIT_TESTS_H
#ifndef IRSMART_HUB_DEBUG_H
static const String DEBUG_DIV = "------------------------------------";
#endif // !IRSMART_HUB_DEBUG_H

void IRS_UnitTests::testAll()
{
	testFirebaseFunctions();
}

void IRS_UnitTests::testFirebaseFunctions()
{
	bool bPassed;
	int numFailed;
	String totalStr;
	int totalNumTests;

	Serial.println(DEBUG_DIV);
	Serial.println(F("Starting FirebaseFunctions tests..."));

	// Test parseHubResultToJson
	numFailed = 0;
	totalNumTests = 0;

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
	numFailed += FirebaseFunctions.test_parseHubResultToJson();
	totalNumTests += 3;
	numFailed += FirebaseFunctions.test_parseJsonToHubAction();
	totalNumTests += 1;
	numFailed += FirebaseFunctions.test_parseHubActionToJson();
	totalNumTests += 2;
#endif //ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H

	totalStr = "Passed (" + String(totalNumTests - numFailed) + "/" + String(totalNumTests) + ")";
	Serial.println(totalStr);

	Serial.println(DEBUG_DIV);
}

#endif