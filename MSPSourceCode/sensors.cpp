#include "Energia.h"
#include "sensors.h"

int ir_fwd = P6_0;
int ir_rev = P6_1;
int loopcount = 10;

int getFwdIR()
{
	return average_value(loopcount, ir_fwd);
}

int getRevIR()
{
	return average_value(loopcount, ir_rev);
}


int average_value(int average_count, int irPin) {
	int sum = 0;
	for (int i=0; i<average_count; i++) {
		int sensor_value = analogRead(irPin); //read the sensor value
		int distance_cm = 8*12343.85*powf(sensor_value, -1.15); //convert reading to cm (better reading)
		sum = sum + distance_cm;
	}
	return(sum/average_count);
	}

void senswait(int delay)
{
	for(int i=0; i<delay; i++) {
		volatile unsigned int x;
		x = 5000;
		do x--;
		while(x !=0);
	}
}
