#ifdef ARDUINO_IR_FUNCTIONS_H
///**
// *
// **/
//uint16_t* ArduinoIRFunctions::parseRawDataString(const char* dataStr, uint16_t rawlen)
//{
//	// Return value
//	uint16_t* rawData = (uint16_t*) calloc(1, rawlen * sizeof(uint16_t));
//	// Next free position in rawData array
//	uint16_t rawDataPos = 0;
//
//	// Points to next char to parse
//	char* pointer = (char *) dataStr;
//
//	// Debug string used to print parse progress
//	char* temp = new char[50];
//
//	// Debug statement
//	#ifdef IR_DEBUG_IR_FUNC
//	Serial.print("Parsing: ");
//	#endif
//
//	// Continue parsing until reach end of dataStr array
//	while (*pointer != '\0')
//	{
//		// Skip values that aren't numbers
//		if (*pointer < '0' || *pointer > '9') 
//		{
//			#ifdef IR_DEBUG_IR_FUNC
//			Serial.print("<"); Serial.print(*pointer); Serial.print(">");
//			#endif
//			pointer++;
//			continue;
//		}
//
//		// Get next number
//		rawData[rawDataPos++] = strtol(pointer, &pointer, 10);
//
//		// Debug statments that print current progress of parse progress
//		#ifdef IR_DEBUG_IR_FUNC
//		sprintf(temp, "%lu", rawData[rawDataPos-1]); Serial.print("("); Serial.print(temp); Serial.print(")"); Serial.print(*pointer);
//		#endif
//	}
//
//	// Debug statement
//	#ifdef IR_DEBUG_IR_FUNC
//	Serial.println("");
//	#endif
//
//	// Free debug string
//	delete[] temp;
//
//	return rawData;
//}

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

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
			FirebaseFunctions.sendError(ERR_TIMEOUT);
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
			
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
#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
			FirebaseFunctions.sendError(ERR_OVERFLOW);
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
			break;
		}

		// Debug statement prints signal results
		#ifdef IR_DEBUG_IR_FUNC
		printResults(&results);
		#endif

		// Only record non-repeat signals
		if (!results.repeat)
		{
			#ifdef IR_DEBUG_IR_FUNC
			Serial.println("Sending...");
			#endif

#ifdef ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H
			FirebaseFunctions.sendRecordedSignal(results OUT);
#endif // ARDUINO_FIREBASE_FUNCTIONS_ESP8266_H	
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
void ArduinoIRFunctions::sendSignal(uint16_t* rawData, uint16_t rawLen, bool bRepeat)
{
	irSender.sendRaw(rawData, rawLen, SEND_FREQUENCY);

	//TODO Implement repeat functionality
}

void ArduinoIRFunctions::init()
{
	irSender.begin();
}

/* ---------- Debugging Functions ---------- */ 

#ifdef IR_DEBUG_IR_FUNC
void ArduinoIRFunctions::printResults(decode_results* results)
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
#endif // IR_DEBUG_IR_FUNC
#endif // ARDUINO_IR_FUNCTIONS_H