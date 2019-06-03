#include "ArduinoIRFunctions.h"

void ArduinoIRFunctions::setDebug(bool debug)
{
	bDEBUG = debug;
}

void ArduinoIRFunctions::readNextSignal()
{
	// Variable to store results
	decode_results results;

	// Start listening for IR signals
	irReceiver.enableIRIn();

	if (bDEBUG) Serial.print("Listeneing for IR Signal");
	long dDelayTimer = millis();
	
	// Get current time for checks against timeout 
	long timeoutTimer = millis();

	// Continue to loop until a valid signal is read or timeout
	while (true)
	{		
		// Check for timeout
		if (millis() - timeoutTimer >= IR_READ_TIMEOUT)
		{
			if (bDEBUG) Serial.println("timeout!");
			
			FirebaseFunctions.sendError(ERR_TIMEOUT);
			break;
		}

		// Debug statment prints "." every second until IR signal has been read
		
		if (bDEBUG && millis() - dDelayTimer >= 1000) { dDelayTimer = millis(); Serial.print("."); }
		

		// Check if complete IR signal has been read
		if (!irReceiver.decode(&results)){
			delay(IR_RECV_MESSAGE_TIMEOUT);
			continue;
		}

		if (bDEBUG) Serial.println("Got results!");

		// Check for overflow
		if (results.overflow) 
		{
			if (bDEBUG) Serial.println("Overflow occurred...");
			FirebaseFunctions.sendError(ERR_OVERFLOW);
			break;
		}

		// Debug statement prints signal results
		#ifdef IR_DEBUG
		SHDebug.printResults(&results);
		#endif

		if (!results.repeat)
		{
			if (bDEBUG) Serial.println("Sending...");
			FirebaseFunctions.sendRecordedSignal(&results);			
			break;
		} 
		else if (bDEBUG) Serial.println("\nIgnoring repeat code...");
	}

	if (bDEBUG) Serial.println("Finished listening for IR signal.");

	irReceiver.disableIRIn();
}

void ArduinoIRFunctions::sendSignal(const String& irSignal, bool bRepeat)
{
	Serial.println("TODO - sendSignal");
}