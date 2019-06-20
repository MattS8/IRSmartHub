#ifdef IRSMARTHUB_UNIT_TESTS
#include "UnitTests.h"

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
	totalNumTests = 5;
	numFailed += FirebaseFunctions.test_parseHubResultToJson();
	numFailed += FirebaseFunctions.test_parseJsonToHubAction();
	numFailed += FirebaseFunctions.test_parseHubActionToJson();

	totalStr = "Passed (" + String(totalNumTests - numFailed) + "/" + String(totalNumTests) + ")";
	Serial.println(totalStr);

	Serial.println(DEBUG_DIV);
}

#endif