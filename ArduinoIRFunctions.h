#ifndef ARDUINO_IR_FUNCTIONS_H
#define ARDUINO_IR_FUNCTIONS_H

#include <IRsend.h>
#include <IRrecv.h>
#include <IRremoteESP8266.h>
#include <IRutils.h>

#ifndef IR_RECV_PIN
#define IR_RECV_PIN 14
#endif

#ifndef IR_BLAST_PIN
#define IR_BLAST_PIN 100
#endif

const uint8_t  IR_RECV_MESSAGE_TIMEOUT = 50;
const uint16_t IR_RECV_BUFFER_SIZE = 1024;
const uint32_t IR_READ_TIMEOUT = 10000;

/* -------------------- Hub Actions -------------------- */
const int IR_ACTION_NONE = 0;
const int IR_ACTION_LEARN = 1;
const int IR_ACTION_SEND = 2;

class ArduinoIRFunctions {
public:
	/**
	 * Listens on IR receiver for the next valid IR signal and
	 * returns the IR code as a string.
	**/
	void readNextSignal();

	void sendSignal(const String& irSignal, bool bRepeat);

	void setDebug(bool debug);

private:
	bool bDEBUG;
	IRrecv irReceiver = IRrecv(IR_RECV_PIN, 
							   IR_RECV_BUFFER_SIZE, 
							   IR_RECV_MESSAGE_TIMEOUT, 
							   true);
	IRsend irSender = IRsend(IR_BLAST_PIN);
};

#endif