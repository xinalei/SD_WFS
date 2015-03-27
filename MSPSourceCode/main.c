#include <msp430.h>
#include <string.h>
#include <stdlib.h>
#include "parsing.h"
#include "motorWorkingWith4Motor.h"

#define BUFFER_SIZE 2000
#define RSSI_SIZE 5

void sendCharsToWifly(char data[]);
void sendCharsToTerm(char data[]);
void fwd();
void rev();
void left();
void right();

char wiflyBuffer[BUFFER_SIZE];
char parsed[BUFFER_SIZE];
char rssiRet[RSSI_SIZE];
int wiflyLastRec=0, networkLastRec=0, passwordCount=0;
int bool_scanning=0,bool_password=0,bool_rssi=0,bool_autonomous=0; // boolean 0=false 1=true
const int rotateSpeedFB = 100;
const int rotateSpeedLR = 255;
const int rotateLengthFB = 3;
const int rotateLengthLR = 3;

int prevRSSI=100;
int currRSSI=100;
int fwdCount=-1;
const int threshold = -20;


int main(void)
{
	WDTCTL = WDTPW + WDTHOLD;                 // Stop WDT

	// initialize A0 uart for comm with the wifly module
	P3SEL = BIT3+BIT4;                        // P3.4,5 = USCI_A0 TXD/RXD
	UCA0CTL1 |= UCSWRST;                      // **Put state machine in reset**
	UCA0CTL1 |= UCSSEL_2;                     // SMCLK
	UCA0BR0 = 9;                              // 1MHz 115200 (see User's Guide)
	UCA0BR1 = 0;                              // 1MHz 115200
	UCA0MCTL |= UCBRS_1 + UCBRF_0;   // Modln UCBRSx=0, UCBRFx=0, over sampling
	UCA0CTL1 &= ~UCSWRST;                     // **Initialize USCI state machine**
	UCA0IE |= UCRXIE;                         // Enable USCI_A0 RX interrupt

	// initialize A1 uart for comm with the bluetooth module
	P4SEL |= BIT4+BIT5;                       // P4.4,5 = USCI_A1 TXD/RXD
	UCA1CTL1 |= UCSWRST;                      // **Put state machine in reset**
	UCA1CTL1 |= UCSSEL_2;                     // SMCLK
	UCA1BR0 = 9;                              // 1MHz 115200 (see User's Guide)
	UCA1BR1 = 0;                              // 1MHz 115200
	UCA1MCTL |= UCBRS_1 + UCBRF_0;   // Modln UCBRSx=0, UCBRFx=0,
	UCA1CTL1 &= ~UCSWRST;                     // **Initialize USCI state machine**
	UCA1IE |= UCRXIE;                         // Enable USCI_A1 RX interrupt

	// Set the pinout for the motor controller.
	setup();

	P4DIR |= 0x80;
	P4OUT = 0x80;

	__bis_SR_register(LPM0_bits + GIE);       // Enter LPM0, interrupts enabled
	__no_operation();                         // For debugger
}


void resetBuffers()
{
	// Reset variables=
	int l;
	for(l=0; l<BUFFER_SIZE; l++) {
	  wiflyBuffer[l] = '\0';
	  parsed[l] = '\0';
	}
	wiflyLastRec = 0;
}

void parseScanData()
{
	// Pass the input to parsing.scan_parse
	scan_parse(wiflyBuffer,parsed);

	// Now that we've got the parsed data, lets send it.
	char *ptr = parsed;
	while('\0' != *ptr)
	{
		while (!(UCA1IFG&UCTXIFG)); // USCI_A1 TX buffer ready?
		UCA1TXBUF = *ptr; // Send char
		ptr++;
	}

	// Reset variables
	bool_scanning = 0;
	resetBuffers();
}

void parseConnectionData()
{
	// Pass the input to password.pw_parse
	char* pos = '\0';
	pos = pw_parse(wiflyBuffer,parsed);

	// First issue the password command.
	sendCharsToWifly("set wlan pass ");

	// Send the actual password
	char *ptr = parsed;
	while('\0' != *ptr)
	{
		while (!(UCA0IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
		UCA0TXBUF = *ptr;
		ptr++;
	}
	sendCharsToWifly("\n\r");

	// Now issue the join command
	sendCharsToWifly("join # ");

	while('\0' != *pos && '\n' != *pos && '\r' != *pos)
	{
		while (!(UCA0IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
		UCA0TXBUF = *pos;
		pos++;
	}
	sendCharsToWifly("\n\r");

	bool_password=0;
	resetBuffers();

	while (!(UCA1IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
	UCA1TXBUF = pos;

}

void parseRSSIData()
{
	// Pass the input to parsing.rssi_parse
	rssi_parse(wiflyBuffer,parsed);

	// Pad data with '*'
	while (!(UCA1IFG&UCTXIFG)); // USCI_A1 TX buffer ready?
	UCA1TXBUF = '*'; // Send char

	// Now we have the rssi value. Send it back to the app.
	char *ptr = parsed;
	int rssiIndex = 0;
	while('\0' != *ptr)
	{
		while (!(UCA1IFG&UCTXIFG)); // USCI_A1 TX buffer ready?
		UCA1TXBUF = *ptr; // Send char
		if(rssiIndex < RSSI_SIZE) rssiRet[rssiIndex++] = *ptr; // add rssi value to specific RSSI array
		ptr++;
	}

	// Pad data with '*'
	while (!(UCA1IFG&UCTXIFG)); // USCI_A1 TX buffer ready?
	UCA1TXBUF = '*'; // Send char


	// Fill rest of rssiIndex with null
	while(rssiIndex<RSSI_SIZE)
	{
		rssiRet[rssiIndex++] = '\0';
	}

	// Reset variables
	if(1 != bool_autonomous) bool_rssi = 0;
	resetBuffers();
}

void sendCharsToWifly(char data[])
{
	int j;
	for(j=0; j<strlen(data); j++)
	{
		while (!(UCA0IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
		UCA0TXBUF = data[j];
	}
}


void sendCharsToTerm(char data[])
{
	int j;
	for(j=0; j<strlen(data); j++)
	{
		while (!(UCA1IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
		UCA1TXBUF = data[j];
	}
}

// Wifly setup: this needs to be called before the join command to join a network.
void wiflySetup()
{
	sendCharsToWifly("set wlan auth 4\n\r");
	sendCharsToWifly("set ip dhcp 1\n\r");
	sendCharsToWifly("set wlan join 0\n\r");
}


int rssiCharArrayToInt()
{
	int indexLSD = RSSI_SIZE -1;
	while(-1 < indexLSD && '\0' == rssiRet[indexLSD])
	{
		indexLSD--;
	}

	int num = 0;
	int multiplier = 1;
	while('-' != rssiRet[indexLSD])
	{
		int digit = (rssiRet[indexLSD] - 0x30);
		num += (multiplier * digit);
		multiplier *=10;
		indexLSD--;
	}
	num *= -1; // make negative

	return num;
}

// USCI_A0 interrupt -- wifly module
#if defined(__TI_COMPILER_VERSION__) || defined(__IAR_SYSTEMS_ICC__)
#pragma vector=USCI_A0_VECTOR
__interrupt void USCI_A0_ISR(void)
#elif defined(__GNUC__)
void __attribute__ ((interrupt(USCI_A0_VECTOR))) USCI_A0_ISR (void)
#else
#error Compiler not supported!
#endif
{
  switch(UCA0IV)
  {
  case 0:break; // no interrupt
  case 2: // RXIFG - received a character!

	  if(1==bool_scanning) // We're expecting scan data to be returned; put it in the buffer.
	  {
		  wiflyBuffer[wiflyLastRec++] = UCA0RXBUF; // put char into buffer.
		  if(wiflyLastRec > BUFFER_SIZE-1)
		  {
			  wiflyLastRec = 0;
		  }

		  /// Checking if END has been received.
		  if(wiflyLastRec > 2 && ('E'==wiflyBuffer[wiflyLastRec-3])
							  && ('N'==wiflyBuffer[wiflyLastRec-2])
							  && ('D'==wiflyBuffer[wiflyLastRec-1]) )
		  {
			  parseScanData(); // Parse the data & then send parsed data to Bluetooth.
		  }
	  }
	  else if(1==bool_rssi)
	  {
		  wiflyBuffer[wiflyLastRec++] = UCA0RXBUF; // put char into buffer.
		  if(wiflyLastRec > BUFFER_SIZE-1)
		  {
			  wiflyLastRec = 0;
		  }

		  /// Checking if dBm has been received.
		  if(wiflyLastRec > 2 && ('d'==wiflyBuffer[wiflyLastRec-3])
							  && ('B'==wiflyBuffer[wiflyLastRec-2])
							  && ('m'==wiflyBuffer[wiflyLastRec-1]) )
		  {
			  parseRSSIData(); // Extract the RSSI value

			  // now rssiRet[] contains the rssi data.
			  if(100!=prevRSSI)
			  {

				  volatile unsigned int jj = 0;
				  int kk;
				  for(kk=0; kk<30; kk++)
				  {
					  P4OUT ^= 0x80;
					  for(jj=0; jj<10000;jj++);
				  }

				  currRSSI = rssiCharArrayToInt();

				  if(currRSSI < threshold) // Negative numbers; continue as long as currRSSI isn't strong enough.
				  {
					  if(currRSSI > prevRSSI)
					  {
						  prevRSSI = currRSSI;
						  fwdCount=1;
					  }
					  else if(currRSSI == prevRSSI)
					  {
						  fwdCount++;
					  }
					  else
					  {
						  int half = fwdCount/2;
						  int y=0;
						  for(y=0; y<half; y++)
						  {
							  rev();
						  }
						  left();
					  }


					  fwd();
					  sendCharsToWifly("show rssi\n\r"); // need to set currRSSI

				  }
				  else // Stopping case!
				  {
					  bool_autonomous = 0;
					  bool_rssi=0;
				  }
			  }
			  else // Then we haven't set it yet. ***NEED TO RESET TO 100 AFTER ALGORITHM ENDS***
			  {
				  prevRSSI = rssiCharArrayToInt();
				  fwd();
				  fwdCount=1;
				  sendCharsToWifly("show rssi\n\r"); // need to set currRSSI
			  }

		  }
	  }
	  else // Send the char received from Wifly to Bluetooth
	  {
		  // send to A1
		  while (!(UCA1IFG&UCTXIFG)); // USCI_A1 TX buffer ready?
		  UCA1TXBUF = UCA0RXBUF;
	  }

	  break;
  case 4: break; // TXIFG - sending a character
  default: break;
  }
}

// USCI_A1 interrupt -- bluetooth module (or terminal if debugging)
#if defined(__TI_COMPILER_VERSION__) || defined(__IAR_SYSTEMS_ICC__)
#pragma vector=USCI_A1_VECTOR
__interrupt void USCI_A1_ISR(void)
#elif defined(__GNUC__)
void __attribute__ ((interrupt(USCI_A1_VECTOR))) USCI_A1_ISR (void)
#else
#error Compiler not supported!
#endif
{
  switch(UCA1IV)
  {
	case 0:break; // no interrupt
	case 2: // RXIFG - received a character!

		if(1==bool_autonomous)
		{
			; // Do nothing (for now). No interrupting the functionality!
		}
		else if(1==bool_password) // We're expecxting a password to be returned; put it in buffer
		{
			if('\n'==UCA1RXBUF) passwordCount++;
			wiflyBuffer[wiflyLastRec++] = UCA1RXBUF; // put char into buffer.
			if(wiflyLastRec > BUFFER_SIZE-1)
			{
				wiflyLastRec = 0;
			}

			// Check for password stopping condition - null
			// ** NOTE password stopping condition being sent from android device.
//			if(wiflyLastRec>0 && 0x18 == wiflyBuffer[wiflyLastRec-1])
			if(passwordCount >= 2) // count newlines
			{
				// Parse data for password and position in list of networks
				parseConnectionData();

			}
		}
		else if('W' == UCA1RXBUF) {
			// we want to directly send commands to the wifly module from now on.
			sendCharsToWifly("$$$"); // Snd "$$$" to wifly
		}
		else if('E' == UCA1RXBUF) {
			// stop sending commands to the wifly module.
			sendCharsToWifly("exit\n\r"); // Send "exit\n\r" to wifly
		}
		else if('S' == UCA1RXBUF) {
			bool_scanning = 1;
			sendCharsToWifly("scan\n\r"); // Send "scan\n\r" to wifly
		}
		else if('R' == UCA1RXBUF)
		{
			sendCharsToWifly("reboot\n\r"); // Send "reboot\n\r" to wifly
		}
		else if('C' == UCA1RXBUF)
		{
			wiflySetup(); // setup wifly up for connecting to networks.
		}
		else if('P' == UCA1RXBUF)
		{
			// Collect next incoming characters (A0_RX) until \0 for network password & position
			bool_password=1;
			passwordCount=0;
		}
		else if('G' == UCA1RXBUF)
		{
			// Show rssi!!
			sendCharsToWifly("show rssi\n\r");
		}
		else if('N' == UCA1RXBUF)
		{
			// Show net
			sendCharsToWifly("show net\n\r");
		}
		else if('I' == UCA1RXBUF)
		{
			// Move forward
			fwd();
		}
		else if('J' == UCA1RXBUF)
		{
			// Turn Left
			left();
		}
		else if('K' == UCA1RXBUF)
		{
			// Reverse
			rev();
		}
		else if('L' == UCA1RXBUF)
		{
			// Turn Right
			right();
		}
		else if('A' == UCA1RXBUF)
		{
			// Begin autonomous functionality.
			bool_autonomous=1;

			P1OUT ^= 0x01;

			// 1. Get RSSI.
			bool_rssi=1;
			sendCharsToWifly("show rssi\n\r");
			// GOTO A0RX under if(1==bool_rssi) for next step.
		}

		break;
	case 4: break; // TXIFG - sending a character
	default: break;
  }
}

void fwd()
{
	forward(rotateSpeedFB, rotateLengthFB);
}

void rev()
{
	reverse(rotateSpeedFB, rotateLengthFB);
}

void left()
{
	rotateLeft(rotateSpeedLR, rotateLengthLR);
}

void right()
{
	rotateRight(rotateSpeedLR, rotateLengthLR);
}
