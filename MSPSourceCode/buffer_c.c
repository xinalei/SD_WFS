//  Buffer_C
//
//  Created by Christina Leichtenschlag on 1/27/15.
//  Copyright (c) 2015 cleichtenschlag. All rights reserved.
//
//  NOTE: if some jack*** has a wifi network with "END:" in the name, this code will break. Highly unlikely, but yeah.

// Function declarations
int getCount(char *);

#include <stdio.h>
#include <stdlib.h> // malloc
#include <string.h> // strlen

#define NETWORKLEN 75
int gotE=0,gotEN=0,gotEND=0,endofscan=0;

void parse(char* input, char **output_str) {
    
    // Sample test input.
    //char *testinp1 = "SCAN:Found 13\n01,01,-59,04,1104,28,c0,20:4e:7f:08:df:85,dad-rules\n02,03,-64,02,1104,28,00,00:30:bd:9b:49:22,basement\n03,10,-71,04,3100,28,00,90:27:e4:5d:fc:a7,URSOMONEY\n04,10,-71,04,3100,28,00,90:27:e4:5d:fc:a7,networkkkk!\nEND:";
    char x = *input;
    char *ptr = input;
    char *ptr_out;
    int numNetworks = getCount(input);
    
    *output_str = (char*) malloc(numNetworks*NETWORKLEN*sizeof(char));
    if(NULL == *output_str)
    {
        return;
    }
    ptr_out = *output_str;
    
    //Look for "SCAN"
    while('S'!= *ptr
            || 'C'!= *(ptr+1)
            || 'A'!= *(ptr+2)
            || 'N'!= *(ptr+3))
    {
        ptr++;
    }
    
    // Found "SCAN". Now, ignore this line (tells you how many networks found)
    while('\n' != x)
    {
        // Update x
        ptr++;
        x=*ptr;
    }
    // Move to the character after the newline.
    ptr++;
    x=*ptr;
    
    int gotListNum=0,commaCount=0,hitSSID=0;
    
    // While loop; continue looking at letters until we find "END:".
    while('\0' != x)
    {
        // Nested if-else statements to determine if we've gotten to the end yet.
        if(0==gotE)
        {
            if('E'==x)
            {
                gotE = 1;
            }
        }
        else // got 'E'
        {
            if(0==gotEN)
            {
                if('N' == x)
                {
                    gotEN=1;
                }
                else
                {
                    gotE = 0; // not the end.
                    *ptr_out = 'E'; // put back 'E'
                    ptr_out++;
                }
            }
            else // got "EN"
            {
                if(0==gotEND)
                {
                    if('D' == x)
                    {
                        gotEND=1;
                    }
                    else
                    {
                        gotE = gotEN = 0; // not the end.
                        *ptr_out = 'E'; // put back "EN"
                        ptr_out++;
                        *ptr_out = 'N';
                        ptr_out++;
                    }
                }
                else // got "END"
                {
                    if(':'==x)
                    {
                        endofscan=1; // MADE IT TO THE END.
                        break;
                    }
                    else
                    {
                        gotE = gotEN = gotEND = 0; // not the end.
                        *ptr_out = 'E'; // put back "END"
                        ptr_out++;
                        *ptr_out = 'N';
                        ptr_out++;
                        *ptr_out = 'D';
                        ptr_out++;
                    }
                }
            }
        }
        
        // If we get to here we haven't gotten to the end, because there's a break at the end.
        if((0==gotListNum || 1==gotListNum) && 0==gotE && 0==gotEN && 0==gotEND) // two digit list num.
        {
            *ptr_out = x; // add digit to output.
            ptr_out++;
            gotListNum++;
        }
        else if(2 == gotListNum)
        {
            *ptr_out = ','; // add comma to delimit
            ptr_out++;
            gotListNum++; // dont want to add too many commas!
        }
        
        // Look at current character; where we at?
        if('\n' == x)
        {
            gotListNum = 0;
            commaCount = 0;
            hitSSID = 0;
            *ptr_out = x; // add newline to output
            ptr_out++;
        }
        else if(',' == x)
        {
            commaCount++;
            if(8 == commaCount)
            {
                // Next char starts SSID
                hitSSID = 1;
            }
        }
        else
        {
            if(1 == hitSSID)
            {
                *ptr_out = x; // add char to output.
                ptr_out++;
            }
        }
        
        // Update x
        ptr++;
        x=*ptr;
    }
    
    // So what happens is that the last 3 characters need to be removed.
    //*output_str[(int)strlen(*output_str)-1] = '\0';
    //free(output_str);
    
    //return *output_str;
}

int getCount(char *string)
{
    char c = *string;
    
    /* After entering a scan command, the wifly module always returns the following
     "SCAN: FOUND ... END:", where ... begins with an nonnegative integer indicating how many networks were found,
     followed by each network and a ton of data, and ending with "END".
     By looping until we find the first lowercase d, we're going to have a pointer just before the number of networks
     that were found.
     */
    while('d' != c)
    {
        string++;
        c = *string;
    }
    
    // c == 'd', thus next character is a space, then we get to the digits.
    // skip the space - increment pointer twice
    string++;
    string++;
    c = *string;
    char digits[2] = {'\0','\0'};
    int i=0;
    while('\n' != c)
    {
        digits[i++] = c;
        string++;
        c = *string;
        if(i>1) break;
    }
    
    int ret = -1; // return value
    
    if('\0' == digits[1])
    {
        // Single digit
        ret = (int) digits[0] - 0x30;
    }
    else
    {
        ret = 10*((int) digits[0] - 0x30);
        ret += (int) digits[1] - 0x30;
    }
    return ret;
}


