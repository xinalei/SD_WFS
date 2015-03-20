//  parsing.c
//
//  Created by Christina Leichtenschlag on 1/27/15.
//  Copyright (c) 2015 cleichtenschlag. All rights reserved.

// Function declarations
void scan_parse(char input[], char output[]);
void getCount(char [], char**);
char pw_parse(char input[], char pw[]);
void rssi_parse(char input[], char rssi[]);

#include <stdio.h>
#include <string.h> // strlen

int gotE=0,gotEN=0,inputLength=0;

void scan_parse(char input[], char output[]) {

	inputLength = (int)strlen(input);
	if(0==inputLength) return;

    char x = *input;
    char *ptr = input;
    char *ptr_out = output;

    // Append to output the number of networks that we scanned plus a newline.
    getCount(input, &ptr_out);


    // Look for "SCAN"
    while('S'!= *ptr
    		&& 'C'!= *(ptr+1)
    		&& 'A'!= *(ptr+2)
    		&& 'N'!= *(ptr+3))
    {
    	ptr++;
    }

    // Found "SCAN", now ignore this line (tells you how many networks found)
    while('\n' != x)
    {
        // Update x
        ptr++;
        x=*ptr;
    }
    // Move to the character after the newline.
    ptr++;
    x=*ptr;

    int commaCount=0,hitSSID=0;

    // While loop; continue looking at letters until we find "END:".
    while('\0' != x)
    {

        // If we get to here we haven't gotten to the end, because there's a break at the end
        // Look at current character; where we at?
    	if('\n' == x)
		{
			commaCount = 0;
			hitSSID = 0;
			*ptr_out = x; // add newline to output
			ptr_out++;
		}
		else if(1 == hitSSID)
		{
			*ptr_out = x; // add char to output.
			ptr_out++;
		}
		else if(',' == x && 8 > commaCount)
		{
			if(0 == commaCount || 2 == commaCount)
			{
				*ptr_out = x; // add comma to output
				ptr_out++;
			}
			commaCount++;
			if(8 == commaCount)
			{
				// Next char starts SSID
				hitSSID = 1;
			}
		}
		else if(0 == commaCount)
		{
			// Right now we're at the channel
			*ptr_out = x; // add char to output
			ptr_out++;

		}
		else if(2 == commaCount)
		{
			// Now we're at the RSSI value
			*ptr_out = x; // add char to output
			ptr_out++;
		}

    	// Update x
		ptr++;
		x=*ptr;
    }

    *ptr_out = '\0'; // end the string by adding null
}

/**
 * @param string[] - the input string, i.e. the raw scan data from the wifly module.
 * @param *out - a pointer to the output string, i.e. the "parsed" scan data that will be sent to Android.
 * This function will identify the number of "found" networks and add that data to the output string
 */
void getCount(char string[], char** out)
{
	int loc = 0;
    char c = string[loc];

    /* After entering a scan command, the wifly module always returns the following
     "SCAN: Found ... END:", where ... begins with an nonnegative integer indicating how many networks were found,
     followed by each network and a ton of data, and ending with "END".
     By looping until we find the first lowercase d, we're going to have a pointer just before the number of networks
     that were found.
     */
    while('d' != c)
    {
        loc++;
        c = string[loc];
    }

    // c == 'd', thus next character is a space, then we get to the digits.
    // skip the space - increment pointer twice
    loc++;
    loc++;
    c = string[loc];
    char digits[2] = {'\0','\0'};
    int i=0;
    while('\n' != c)
    {
        digits[i++] = c;
        loc++;
        c = string[loc];
        if(i>1) break;
    }

    if('\0' == digits[1])
    {
        **out = digits[0];
        (*out)++;
        **out = '\n';
        (*out)++;
    }
    else
    {
        **out = digits[0];
        (*out)++;
        **out = digits[1];
        (*out)++;
        **out = '\n';
        (*out)++;
    }

}

char pw_parse(char input[], char pw[])
{
	int inpLength, newlineCount=0;
	inpLength = (int)strlen(input);
	if(0==inpLength) return  '\0';

	// Sample test input.
	char x = *input;
	char *ptr = input;
	char *ptr_pw = pw;
	char position  = '\0'; // this will get returned.

	while('\0' != x)
	{
		if('\n' == x || '\r' == x)
		{
			newlineCount++;
			if(2 == newlineCount)
			{
				// Reached 2 newlines, thus end of input.
				break;
			}
		}
		else
		{
			if(0==newlineCount)
			{
				*ptr_pw = x;
				ptr_pw++;
			}
			else if(1==newlineCount)
			{
				position = x;
			}
		}
        // Update x
        ptr++;
        x=*ptr;
	}

	*ptr_pw = '\0';
	ptr_pw++;

	return position; // return the int position in the list of networks. used for join command.
}


void rssi_parse(char input[], char rssi[])
{
	int inpLength, newlineCount=0;
	inpLength = (int)strlen(input);
	if(0==inpLength) return;

	// Sample test input.
	char x = *input;
	char *ptr = input;
	char *ptr_rssi = rssi;

	int bool_LP=0;

	while('\0' != x)
	{
		if(0==bool_LP && '('==x)
		{
			// Found the left parenthese, set bool.
			bool_LP=1;
		}
		else if(1==bool_LP && ')'!=x)
		{
			// This is the RSSI value; append it to the output array.
			*ptr_rssi = x;
			ptr_rssi++;
		}
		else if(1==bool_LP && ')'==x)
		{
			// Found the right parenthese & stopping condition!
			break;
		}

		// Update x
		ptr++;
		x=*ptr;
	}

	*ptr_rssi = '\0';

	return;
}
