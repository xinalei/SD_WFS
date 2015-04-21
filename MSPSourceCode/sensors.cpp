#include "Energia.h"
#include "sensors.h"

int ir_fwd1 = P6_0;
int ir_fwd2 = P6_1;
int ir_rev1 = P6_4;
int ir_rev2 = P7_0;
int loopcount = 5;

int getFwd1IR()
{
	return average_value(loopcount, ir_fwd1);
}

int getFwd2IR()
{
	return average_value(loopcount, ir_fwd2);
}

int getRev1IR()
{
	return average_value(loopcount, ir_rev1);
}

int getRev2IR()
{
	return average_value(loopcount, ir_rev2);
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
