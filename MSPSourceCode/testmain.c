//
//  testmain.c
//  Buffer_C
//
//  Created by Christina Leichtenschlag on 1/29/15.
//  Copyright (c) 2015 cleichtenschlag. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include "buffer_c.h"

int main()
{
    char testinp1[] = "can\n\n<4.00>SCAN:Found 13\n01,01,-59,04,1104,28,c0,20:4e:7f:08:df:85,dad-rules\n02,03,-64,02,1104,28,00,00:30:bd:9b:49:22,basement\n03,10,-71,04,3100,28,00,90:27:e4:5d:fc:a7,URSOMONEY\n04,10,-71,04,3100,28,00,90:27:e4:5d:fc:a7,networkkkk!\nEND:";
    
    char *toBeSent = NULL;
    
    parse(testinp1, &toBeSent);
    if('\0' == NULL) printf("FUCK");
    printf("\n\ntoBeSent length = %lu\n",strlen(toBeSent));
    
    char c = *toBeSent;
    while('\0' != c)
    {
        printf("%c",c);
        toBeSent++;
        c = *toBeSent;
    }
    
    printf("\n--------\n");
    
    int k;
    for(k=0; k<strlen(toBeSent); k++)
    {
        printf("%c",toBeSent[k]);
    }
    
    return 0;
}