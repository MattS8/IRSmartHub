#include "ArduinoIRFunctions.h"

/**
 *
 **/
uint16_t* ArduinoIRFunctions::parseRawDataString(const char* dataStr, uint16_t rawlen)
{
	// Return value
	uint16_t* rawData = (uint16_t*) calloc(1, rawlen * sizeof(uint16_t));
	// Next free position in rawData array
	uint16_t rawDataPos = 0;

	// Points to next char to parse
	char* pointer = (char *) dataStr;

	// Debug string used to print parse progress
	char* temp = new char[50];

	// Debug statement
	if (bDEBUG) Serial.print("Parsing: ");

	// Continue parsing until reach end of dataStr array
	while (*pointer != '}')
	{
		// Skip values that aren't numbers
		if (*pointer < '0' || *pointer > '9') 
		{
			if (bDEBUG) { Serial.print("<"); Serial.print(*pointer); Serial.print(">"); }
			pointer++;
			continue;
		}

		// Get next number
		rawData[rawDataPos++] = strtol(pointer, &pointer, 10);

		// Debug statments that print current progress of parse progress
		if (bDEBUG) { sprintf(temp, "%lu", rawData[rawDataPos-1]); Serial.print("("); Serial.print(temp); Serial.print(")"); Serial.print(*pointer); }
	}

	// Debug statement
	if (bDEBUG) Serial.println("");

	// Free debug string
	delete[] temp;

	return rawData;
}

/**
 *
 **/
void ArduinoIRFunctions::setDebug(bool debug)
{
	bDEBUG = debug;
}

/**
 *
 **/
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

		// Debug statment
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
		if (bDEBUG) SHDebug.printResults(&results);
		#endif

		// Only record non-repeat signals
		if (!results.repeat)
		{
			if (bDEBUG) Serial.println("Sending...");
			FirebaseFunctions.sendRecordedSignal(&results);			
			break;
		} 
		else if (bDEBUG) Serial.println("\nIgnoring repeat code...");
	}

	// Debug statement
	if (bDEBUG) Serial.println("Finished listening for IR signal.");

	// Stop listening for IR signals
	irReceiver.disableIRIn();
}

/**
 *
 **/
void ArduinoIRFunctions::sendSignal(const String& rawDataStr, uint16_t rawLen, bool bRepeat)
{
	uint16_t* rawData = parseRawDataString(rawDataStr.c_str(), rawLen);
	irSender.sendRaw(rawData, rawLen, SEND_FREQUENCY);

	//TODO Implement repeat functionality
}

void ArduinoIRFunctions::init()
{
	irSender.begin();
}