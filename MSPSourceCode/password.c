//  password.c
//
//  Created by Christina Leichtenschlag on 1/27/15.
//  Copyright (c) 2015 cleichtenschlag. All rights reserved.

// Function declarations

#include <stdio.h>
#include <string.h> // strlen

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
