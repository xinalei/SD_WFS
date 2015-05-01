#include <msp430.h>
#include <string.h>
#include <stdlib.h>
#include "parsing.h"
#include "motorWorkingWith4Motor.h"
#include "sensors.h"

#define BUFFER_SIZE 2000
#define RSSI_SIZE 5
#define AUTO_BUF_SIZE 40

// Function headers
void sendCharsToWifly(char data[]);
void sendCharsToTerm(char data[]);
void fwd();
void rev();
void left();
void right();

// Char array storage
char wiflyBuffer[BUFFER_SIZE];
char parsed[BUFFER_SIZE];
char rssiRet[RSSI_SIZE];
int wiflyLastRec=0, networkLastRec=0, passwordCount=0;
// Booleans
int bool_scanning=0,bool_password=0,bool_autonomous=0,bool_manualRSSI=0; // boolean 0=false 1=true
// Constants
const int rotateSpeedFB = 200;
//const int rotateSpeedLR = 200;		//Hardwood
const int rotateSpeedLR = 255;			//carpet
const int rotateLengthFB = 3;
const int rotateLengthLR = 6;
const int persistentThreshold = -35;
// Autonomous algorithm variables
int prevRSSI=100;
int currRSSI=100;
int localMaxRSSI=-100;
int rssiDiff = 5;
int fwdCount=1;
int sensingDist=35; //cm
int backtrackCount=0;
int currThreshold=1;

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

	currThreshold=persistentThreshold;

	P1DIR |= 0x03; // set green led as output 1.0 & white led as output 1.1
	P4DIR |= 0x80; // set blue led as output 4.7
	P1OUT |= 0x01; // turn on green led
	P4OUT &= ~0x80; // turn off blue led
	P1OUT &= ~0x02; // turn off white led


	__bis_SR_register(LPM0_bits + GIE);       // Enter LPM0, interrupts enabled
	__no_operation();                         // For debugger
}

void toggleWhite()
{
	P1OUT ^= 0x01;
}

void toggleBlue()
{
	P1OUT ^= 0x02;
}

void toggleGreen()
{
	P4OUT ^= 0x80;
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
	UCA1TXBUF = *pos;

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


void rssiIntToCharArray()
{
	char temp[RSSI_SIZE];
	int l, tempIndex=0, retIndex=1, mod;
	for(l=0; l<RSSI_SIZE; l++) {
		temp[l] = '\0';
		rssiRet[l] = '\0';
	}
	rssiRet[0] = '-';
	
	currRSSI *= -1; // first make positive // was localMaxRSSI
	while(currRSSI > 0)
	{
	    mod	= currRSSI % 10;
		temp[tempIndex++] = (0x30 + mod); // could throw but shouldn't. ever.
		currRSSI /= 10;
	}
	
	for(l=(tempIndex-1); l>-1; l--)
	{
		rssiRet[retIndex++] = temp[l];
	}

	sendCharsToTerm(rssiRet); // Send the rssi to the app
	
}

void endAutoAlg()
{
	// currRSSI is less than threshold. Therefore return RSSI i.e. currRSSI
	bool_autonomous = 0;
	P1OUT = 0x00;
	prevRSSI=100; // reset so we can run algorithm again
	backtrackCount=0;
	currThreshold = persistentThreshold;

	/// So we want to signal to the application that we're done seeking.
	sendCharsToTerm("@fin@");
	rssiIntToCharArray(); // sends the current rssi to the app
	sendCharsToTerm("@");
	
	// Gotta do this one last.
	localMaxRSSI=-100;
	currRSSI=100; // reset just in case.


	P1OUT &= ~0x02; // turn off white led
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

	  if(1==bool_scanning) // We're expecting scan data to be returned; put it in the buffer & then parse it.
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
	  else if(1==bool_autonomous) // We're in autonomous mode! Herein lies the code for the autonomous algorithm.
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
			  parseRSSIData(); // Extract the RSSI value; also sends RSSI back to the app '*'-delimited
			  toggleBlue();
			  // now rssiRet[] contains the rssi data.
			  if(100!=prevRSSI)
			  {
//				  // Toggles blue LED. Also need this so the robot doesn't go charging
//				  // & moving too quickly for the algorithm
//				  volatile unsigned int jj = 0;
//				  int kk;
//				  for(kk=0; kk<4; kk++)
//				  {
//					  toggleBlue();
//					  for(jj=0; jj<10000;jj++);
//				  }

				  // Convert the rssi value to an int. Now let's compare.
				  currRSSI = rssiCharArrayToInt();

				  if(currRSSI < currThreshold) // Negative numbers; continue as long as currRSSI isn't strong enough.
				  {
					  if(currRSSI > prevRSSI)
					  {
						  prevRSSI = currRSSI;
						  fwdCount=1;
						  
						  // Now to prevent infinite algorithm
						  localMaxRSSI = prevRSSI; // set local max
						  backtrackCount=0; // reset count
					  }
					  else if((prevRSSI-rssiDiff) <= currRSSI && prevRSSI >= currRSSI) 
					  {
						  // If the RSSI is *about* the same, keep going. Get some pretty severe fluctuations.
						  fwdCount++;
						  backtrackCount++;
					  }
					  else // RSSI took a severe turn for the worse. Reverse!
					  {
						  int half = fwdCount/2;
						  int y=0;
						  for(y=0; y<half; y++)
						  {
							  rev(); // move backwards! (i.e. reverse)
							  backtrackCount++;
						  }
						  left(); // move left
						  backtrackCount++;
						  fwdCount=1;
					  }

					  // We just moved according to autonomous algorithm.
					  // Check for infinite case, and reset threshold if necessary
					  fwd(); // move forward!
					  backtrackCount++;

					  if(backtrackCount >= AUTO_BUF_SIZE) // time to lower our standards
					  {
						  // Need to reset the current threshold.
						  sendCharsToTerm("reset");
						  currThreshold = localMaxRSSI-2;
						  backtrackCount=0;
					  }

					  // Now continue
					  sendCharsToWifly("show rssi\n\r"); // need to set currRSSI

				  }
				  else // Stopping case!
				  {
					  endAutoAlg();
				  }
			  }
			  else // Then we haven't set it yet. ***NEED TO RESET TO 100 AFTER ALGORITHM ENDS***
			  {
				  prevRSSI = rssiCharArrayToInt();
			      if(prevRSSI > currThreshold)
				  {
				      // We're done before the algorithm even started. 
			    	  currRSSI = prevRSSI;
					  endAutoAlg();
				  }
				  else // We need to run the autonomous algorithm.
				  {
					  localMaxRSSI = prevRSSI;
					  fwd(); // move forward!
				      fwdCount=1;
				      backtrackCount++;
				      sendCharsToWifly("show rssi\n\r"); // need to set currRSSI
				  }
				  
			  }

		  }
	  }
	  else if(1==bool_manualRSSI)
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
			  parseRSSIData(); // Extract the RSSI value; also sends RSSI back to the app '*'-delimited
			  bool_manualRSSI=0;
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

			// Check for password stopping condition - received 2 newline characters.
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
			// Collect next incoming characters (A0_RX) until 2*(\n) for network password & position
			bool_password=1;
			passwordCount=0;
		}
		else if('G' == UCA1RXBUF)
		{
			// Show rssi!!
			bool_manualRSSI=1;
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
			bool_manualRSSI=1;
			sendCharsToWifly("show rssi\n\r"); // Also update RSSI
		}
		else if('J' == UCA1RXBUF)
		{
			// Turn Left
			left();
			bool_manualRSSI=1;
			sendCharsToWifly("show rssi\n\r"); // Also update RSSI
		}
		else if('K' == UCA1RXBUF)
		{
			// Reverse
			rev();
			bool_manualRSSI=1;
			sendCharsToWifly("show rssi\n\r"); // Also update RSSI
		}
		else if('L' == UCA1RXBUF)
		{
			// Turn Right
			right();
			bool_manualRSSI=1;
			sendCharsToWifly("show rssi\n\r"); // Also update RSSI
		}
		else if('A' == UCA1RXBUF)
		{
			// Begin autonomous functionality.
			bool_autonomous=1;

			P1OUT |= 0x02; // turn on white led

			// 1. Get RSSI.
			sendCharsToWifly("show rssi\n\r");
			// GOTO A0RX under if(1==bool_autonomous) for next step.
		}

		break;
	case 4: break; // TXIFG - sending a character
	default: break;
  }
}

void fwd()
{
	if(getFwd1IR() > sensingDist && getFwd2IR() > sensingDist)
	{
		forward(rotateSpeedFB, rotateLengthFB);
	}
	else
	{

		// Need to make sure that the object wasn't temporary
//			wait(2);
		if((sensingDist+1) > getFwd1IR() || (sensingDist+1) > getFwd2IR())
		{
			  // Toggles blue LED. Also need this so the robot doesn't go charging
			  // & moving too quickly for the algorithm
			  volatile unsigned int jj = 0;
			  int kk;
			  for(kk=0; kk<4; kk++)
			  {
				  toggleBlue();
				  for(jj=0; jj<10000;jj++);
			  }
			// Need to avoid the obstacle. Only if in autonomous mode and not backtracking.
			if(1==bool_autonomous)
			{
				// Just back up and turn left. Probably a terrible idea, but we're doing it.
				rev();
				left();
				backtrackCount++;
				backtrackCount++;
				fwdCount=1;
			}
		}
		else
		{
			forward(rotateSpeedFB, rotateLengthFB);
		}
	}
}

void rev()
{
	if(getRev1IR() > sensingDist && getRev2IR() > sensingDist)
	{
		reverse(rotateSpeedFB, rotateLengthFB);
	}
	else
	{
		  // Toggles blue LED. Also need this so the robot doesn't go charging
		  // & moving too quickly for the algorithm
		  volatile unsigned int jj = 0;
		  int kk;
		  for(kk=0; kk<4; kk++)
		  {
			  toggleBlue();
			  for(jj=0; jj<10000;jj++);
		  }
	}
}

void left()
{
	rotateLeft(rotateSpeedLR, rotateLengthLR);
}

void right()
{
	rotateRight(rotateSpeedLR, rotateLengthLR);
}
