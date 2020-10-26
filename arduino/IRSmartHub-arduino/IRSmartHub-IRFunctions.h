#ifndef IRSMARTHUB_IRFUNCTIONS_H
#define IRSMARTHUB_IRFUNCTIONS_H

#define FIREBASE_FUNCTIONS_ENABLED

#include "IRsend.h"
#include "IRrecv.h"
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

class IRSmartHubIRFunctions {
public:
	/**
	 * Listens on IR receiver for the next valid IR signal and
	 * returns the IR code as a string.
	**/
	void readNextSignal();

	void sendSignal(uint16_t* rawDataStr, uint16_t rawlen, bool bRepeat);

	void init();

private:
	uint16_t* parseRawDataString(const char* dataStr, uint16_t rawlen);

	IRrecv irReceiver = IRrecv(IR_RECV_PIN, 
							   IR_RECV_BUFFER_SIZE, 
							   IR_RECV_MESSAGE_TIMEOUT, 
							   true);
	IRsend irSender = IRsend(IR_BLAST_PIN);
};

#endif