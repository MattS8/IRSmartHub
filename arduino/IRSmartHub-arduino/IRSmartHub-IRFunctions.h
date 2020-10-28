#ifndef IRSMARTHUB_IRFUNCTIONS_H
#define IRSMARTHUB_IRFUNCTIONS_H

#include "IRsend.h"
#include "IRrecv.h"						// Used to send IR data from decode_results
#include "IRremoteESP8266.h"

#ifndef IR_RECV_PIN
#define IR_RECV_PIN 14
#endif

#ifndef IR_BLAST_PIN
#define IR_BLAST_PIN 12
#endif

const uint8_t  IR_RECV_MESSAGE_TIMEOUT = 50;
const uint16_t IR_RECV_BUFFER_SIZE = 1024;
const uint32_t IR_READ_TIMEOUT = 10000;
const uint16_t SEND_FREQUENCY = 38;

/* -------------------- 
    Result Codes
    -------------------- */
static const int RES_SEND_SIG	= 700;
static const int RES_SEND_SUCC	= 701;
static const int ERR_UNKNOWN	= 800;
static const int ERR_TIMEOUT	= 801;
static const int ERR_OVERFLOW	= 802;

typedef struct ReadResult {
	decode_results results;
	int resultCode;
} ReadResult;

class IRSmartHubIRFunctions {
public:
	/**
	 * Listens on IR receiver for the next valid IR signal and
	 * returns the IR code as a string.
	**/
	void readNextSignal();

	void sendSignal(uint16_t* rawData, uint16_t rawlen, bool bRepeat);

	void init();

	String resultToHexidecimal(const decode_results& result);

	String rawDataToString(volatile uint16_t* rawbuf, uint16_t rawLen, uint16_t startPos, bool limitToChunk);
	
	uint16_t getCorrectedRawLength(const decode_results& results);

	uint16_t getCorrectedChunkCount(uint16_t rawLen);

	String uint64ToString(uint64_t input, uint8_t base = 10);

	uint16_t* parseRawDataString(const char* dataStr, uint16_t* rawData, uint16_t startPos);

	ReadResult readResult;

private:
	IRrecv irReceiver = IRrecv(IR_RECV_PIN, 
							   IR_RECV_BUFFER_SIZE, 
							   IR_RECV_MESSAGE_TIMEOUT, 
							   true);
	IRsend irSender = IRsend(IR_BLAST_PIN);
};

#endif