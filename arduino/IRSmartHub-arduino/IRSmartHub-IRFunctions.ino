#include "IRSmartHub-IRFunctions.h"

/**
 *
 **/
#define RD_NXT_SIG_DEBUG
void IRSmartHubIRFunctions::readNextSignal()
{
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
			Serial.println();
			Serial.println("timeout!");
#endif
			readResult.resultCode = ERR_TIMEOUT;
			break;
		}

// Debug statment prints "." every second until IR signal has been read
#ifdef RD_NXT_SIG_DEBUG
		if (millis() - dDelayTimer >= 1000)
		{
			dDelayTimer = millis();
			Serial.print(".");
		}
#endif
		// Check if complete IR signal has been read
		if (!irReceiver.decode(&(readResult.results)))
		{
			delay(IR_RECV_MESSAGE_TIMEOUT);
			continue;
		}

// Debug statment
#ifdef RD_NXT_SIG_DEBUG
		Serial.println();
		Serial.println("Got results!");
#endif
		// Check for overflow
		if (readResult.results.overflow)
		{
#ifdef RD_NXT_SIG_DEBUG
			Serial.println();
			Serial.println("Overflow occurred...");
#endif
			readResult.resultCode = ERR_OVERFLOW;
			break;
		}

// Debug statement prints signal results
#ifdef RD_NXT_SIG_DEBUG
		printResults(&(readResult.results));
#endif
		// Only record non-repeat signals
		if (!readResult.results.repeat)
		{
#ifdef RD_NXT_SIG_DEBUG
			Serial.println("Sending...");
#endif
			readResult.resultCode = RES_SEND_SIG;
			break;
		}
		else
		{
#ifdef RD_NXT_SIG_DEBUG
			Serial.println();
			Serial.println("Ignoring repeat code...");
#endif
		}
	}

// Debug statement
#ifdef RD_NXT_SIG_DEBUG
	Serial.println("Finished listening for IR signal.");
#endif
	// Stop listening for IR signals
	irReceiver.disableIRIn();
}

String IRSmartHubIRFunctions::resultToHexidecimal(const decode_results& result) {
  String output = "";
  output += uint64ToString(result.value, 16);

  return output;
}

/**
  *	Converts the raw data from array of uint16_t to a string.
  * Note: Trying to convert more than CHUNK_SIZE could lead to
  *	memory instability.
 **/
String IRSmartHubIRFunctions::rawDataToString(volatile uint16_t* rawbuf, uint16_t rawLen, uint16_t startPos, bool limitToChunk)
{
	String output = "";
	// Dump data

	uint32_t usecs;
	for (uint16_t i = startPos; i < rawLen && (i - startPos < CHUNK_SIZE || !limitToChunk); i++)
	{
		// If data is > UINT16_MAX, add multiple entries
		for (usecs = rawbuf[i] * kRawTick; usecs > UINT16_MAX; usecs -= UINT16_MAX)
		{
			output += uint64ToString(UINT16_MAX);
			if (i % 2)
				output += F(", 0,  ");
			else
				output += F(",  0, ");
		}
		output += uint64ToString(usecs, 10);
		if (i < rawLen - 1)
			output += F(", ");						// ',' not needed on the last one
		if (i % 2 == 0)
			output += ' ';							// Extra if it was even.
	}

	// End declaration
	output += F("\0");

	return output;
}

/**
 *	Return the corrected length of a 'raw' format array structure after over-large values are
 *	converted into multiple entries. (Function logic from IRutils: https://github.com/markszabo/IRremoteESP8266)
**/
uint16_t IRSmartHubIRFunctions::getCorrectedRawLength(const decode_results& results) 
{
	uint16_t extended_length = results.rawlen - 1;
	for (uint16_t i = 0; i < results.rawlen - 1; i++) 
	{
		uint32_t usecs = results.rawbuf[i] * kRawTick;
		// Add two extra entries for multiple larger than UINT16_MAX it is.
		extended_length += (usecs / (UINT16_MAX + 1)) * 2;
	}

	return extended_length;
}

/**
  *	Gets the number of chunks needed based on the 
  *	length of the rawData array. This function
  *	always rounds up to ensure enough chunks are
  *	allocated.
 **/
uint16_t IRSmartHubIRFunctions::getCorrectedChunkCount(uint16_t rawLen)
{
	uint16_t count = ceil(rawLen / CHUNK_SIZE);

	return count * CHUNK_SIZE < rawLen ? count + 1 : count;
}

String IRSmartHubIRFunctions::uint64ToString(uint64_t input, uint8_t base)
{
	String result = "";
	// Check we have a base that we can actually print.
	// i.e. [0-9A-Z] == 36
	if (base < 2 || base > 36) base = 10;

	do
	{
		char c = input % base;
		input /= base;

		c += c < 10 ? '0' : 'A' - 10;

		result = c + result;
	} while (input);

	return result;
}

/** -------- DEBUG FUNCTIONS -------- **/

#if defined(RD_NXT_SIG_DEBUG)
#include "IRutils.h"
void printResults(const decode_results *results)
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