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
	#ifdef IR_DEBUG_IR_FUNC
	Serial.print("Parsing: ");
	#endif

	// Continue parsing until reach end of dataStr array
	while (*pointer != '\0')
	{
		// Skip values that aren't numbers
		if (*pointer < '0' || *pointer > '9') 
		{
			#ifdef IR_DEBUG_IR_FUNC
			Serial.print("<"); Serial.print(*pointer); Serial.print(">");
			#endif
			pointer++;
			continue;
		}

		// Get next number
		rawData[rawDataPos++] = strtol(pointer, &pointer, 10);

		// Debug statments that print current progress of parse progress
		#ifdef IR_DEBUG_IR_FUNC
		sprintf(temp, "%lu", rawData[rawDataPos-1]); Serial.print("("); Serial.print(temp); Serial.print(")"); Serial.print(*pointer);
		#endif
	}

	// Debug statement
	#ifdef IR_DEBUG_IR_FUNC
	Serial.println("");
	#endif

	// Free debug string
	delete[] temp;

	return rawData;
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

	#ifdef IR_DEBUG_IR_FUNC
	Serial.print("Listeneing for IR Signal");
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
			#ifdef IR_DEBUG_IR_FUNC
			Serial.println("timeout!");
			#endif

			FirebaseFunctions.sendError(ERR_TIMEOUT);
			break;
		}

		// Debug statment prints "." every second until IR signal has been read
		#ifdef IR_DEBUG_IR_FUNC
		if (millis() - dDelayTimer >= 1000) { dDelayTimer = millis(); Serial.print("."); }
		#endif

		// Check if complete IR signal has been read
		if (!irReceiver.decode(&results)){
			delay(IR_RECV_MESSAGE_TIMEOUT);
			continue;
		}

		// Debug statment
		#ifdef IR_DEBUG_IR_FUNC
		Serial.println("Got results!");
		#endif

		// Check for overflow
		if (results.overflow) 
		{
			#ifdef IR_DEBUG_IR_FUNC
			Serial.println("Overflow occurred...");
			#endif
			FirebaseFunctions.sendError(ERR_OVERFLOW);
			break;
		}

		// Debug statement prints signal results
		#ifdef IR_DEBUG_IR_FUNC
		SHDebug.printResults(&results);
		#endif

		// Only record non-repeat signals
		if (!results.repeat)
		{
			#ifdef IR_DEBUG_IR_FUNC
			Serial.println("Sending...");
			#endif

			FirebaseFunctions.sendRecordedSignal(&results);			
			break;
		} 
		#ifdef IR_DEBUG_IR_FUNC
		Serial.println("\nIgnoring repeat code...");
		#endif
	}

	// Debug statement
	#ifdef IR_DEBUG_IR_FUNC
	Serial.println("Finished listening for IR signal.");
	#endif

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