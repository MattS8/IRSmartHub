#include "IRSmartHub-IRFunctions.h"

/**
 *
 **/
void IRSmartHubIRFunctions::readNextSignal()
{
	// Variable to store results
	decode_results results;

	// Start listening for IR signals
	irReceiver.enableIRIn();

	#ifdef RD_NXT_SIG_DEBUG
	Serial.print("Listeneing for IR Signal...");
	#endif

	long dDelayTimer = millis();
	
	// Get current time for checks against timeout 
	long timeoutTimer = millis();

	// Continue to loop until a valid signal is read or timeout
	while (true)
	{		
		// Check for timeout
		if (millis() - timeoutTimer >= IR_READ_TIMEOUT)
		{
			#ifdef RD_NXT_SIG_DEBUG
            Serial.println()
			Serial.println("timeout!");
			#endif

            #ifdef FIREBASE_FUNCTIONS_ENABLED
			FirebaseFunctions.sendError(ERR_TIMEOUT);
            #endif
			
			break;
		}

		// Debug statment prints "." every second until IR signal has been read
		#ifdef RD_NXT_SIG_DEBUG
		if (millis() - dDelayTimer >= 1000) { dDelayTimer = millis(); Serial.print("."); }
		#endif

		// Check if complete IR signal has been read
		if (!irReceiver.decode(&results)){
			delay(IR_RECV_MESSAGE_TIMEOUT);
			continue;
		}

		// Debug statment
		#ifdef RD_NXT_SIG_DEBUG
        Serial.println();
		Serial.println("Got results!");
		#endif

		// Check for overflow
		if (results.overflow) 
		{
			#ifdef RD_NXT_SIG_DEBUG
            Serial.println();
			Serial.println("Overflow occurred...");
			#endif

            #ifdef FIREBASE_FUNCTIONS_ENABLED
			FirebaseFunctions.sendError(ERR_OVERFLOW);
            #endif

			break;
		}

		// Debug statement prints signal results
		#ifdef RD_NXT_SIG_DEBUG
		printResults(&results);
		#endif

		// Only record non-repeat signals
		if (!results.repeat)
		{
			#ifdef RD_NXT_SIG_DEBUG
			Serial.println("Sending...");
			#endif

#ifdef FIREBASE_FUNCTIONS_ENABLED
			FirebaseFunctions.sendRecordedSignal(results OUT);
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H	
			break;
		} 
		#ifdef RD_NXT_SIG_DEBUG
        Serial.println();
		Serial.println("Ignoring repeat code...");
		#endif
	}

	// Debug statement
	#ifdef RD_NXT_SIG_DEBUG
	Serial.println("Finished listening for IR signal.");
	#endif

	// Stop listening for IR signals
	irReceiver.disableIRIn();
}

/** -------- DEBUG FUNCTIONS -------- **/

#if defined(RD_NXT_SIG_DEBUG)
void printResults(decode_results* results)
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
#endif