#include <msp430.h>
#include <string.h>
#include <stdlib.h>
#include "buffer_c.h"
#include "password.h"
#include "motorWorkingWith4Motor.h"

#define BUFFER_SIZE 2000

void sendCharsToWifly(char data[]);
void sendCharsToTerm(char data[]);

char wiflyBuffer[BUFFER_SIZE];
char parsed[BUFFER_SIZE];
int wiflyLastRec=0, networkLastRec=0, passwordCount=0;
int bool_scanning=0,bool_password=0,bool_join=0; // boolean 0=false 1=true
const int rotateSpeed = 150;
const int rotateLength = 5;

volatile int wiflyCommand = 0; // false

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

	P1DIR = 0x01;
	P1OUT ^= 0x01;

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
	// Pass the input to buffer_c.parse
	parse(wiflyBuffer,parsed);

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
	char pos = ')';
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
	while (!(UCA0IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
	UCA0TXBUF = pos;
	sendCharsToWifly("\n\r");

	bool_password=0;
	resetBuffers();

	while (!(UCA1IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
	UCA1TXBUF = pos;

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
		  wiflyCommand=0;
		  wiflyBuffer[wiflyLastRec++] = UCA0RXBUF; // put char into buffer.
		  if(wiflyLastRec > BUFFER_SIZE-1)
		  {
			  wiflyLastRec = 0;
		  }

		  /// Checking if END has been received.
		  if(wiflyLastRec > 2 && ('D'==wiflyBuffer[wiflyLastRec-1])
							  && ('N'==wiflyBuffer[wiflyLastRec-2])
							  && ('E'==wiflyBuffer[wiflyLastRec-3]) )
		  {
			  wiflyCommand=1; // Redirect all chars to Wifly again
			  parseScanData(); // Parse the data & then send parsed data to Bluetooth.
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

		if(1==bool_password) // We're expecxting a password to be returned; put it in buffer
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
			wiflyCommand = 1;
			sendCharsToWifly("$$$"); // Snd "$$$" to wifly
		}
		else if('E' == UCA1RXBUF) {
			// stop sending commands to the wifly module.
			wiflyCommand = 0;
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
			forward(rotateSpeed, rotateLength);
		}
		else if('J' == UCA1RXBUF)
		{
			// Turn Left
			rotateLeft(rotateSpeed, rotateLength);
		}
		else if('K' == UCA1RXBUF)
		{
			// Reverse
			reverse(rotateSpeed, rotateLength);
		}
		else if('L' == UCA1RXBUF)
		{
			// Turn Right
			rotateRight(rotateSpeed, rotateLength);
		}
//		else {
//
//			// If wifly is in command mode and isn't one of the above preset functions,
//			//  send the character to the wifly module.
//			if(1 == wiflyCommand) {
//				while (!(UCA0IFG&UCTXIFG)); // USCI_A0 TX buffer ready?
//				UCA0TXBUF = UCA1RXBUF; // send character to wifly module
//			}
//		}

		break;
	case 4: break; // TXIFG - sending a character
	default: break;
  }
}

