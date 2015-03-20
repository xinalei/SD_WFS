#include "Energia.h"
#include "motorWorkingWith4Motor.h"

const int motor1Pin1 = P2_5;    // H-bridge leg 1 (pin 2, 1A)
const int motor1Pin2 = P2_4;    // H-bridge leg 2 (pin 7, 2A)
const int motor2Pin3 = P1_5;
const int motor2Pin4 = P1_4;
//const int enablePin = 9;    // H-bridge enable pin

void setup() {
  
    // set all the other pins you're using as outputs:
    pinMode(motor1Pin1, OUTPUT);
    pinMode(motor1Pin2, OUTPUT);
    pinMode(motor2Pin3, OUTPUT);
    pinMode(motor2Pin4, OUTPUT);
    
    //pinMode(enablePin, OUTPUT);
 
    // set enablePin high so that motor can turn on:
    // digitalWrite(enablePin, HIGH);
  }
  

void wait(int delay)
{
	for(int i=0; i<delay; i++) {
		volatile unsigned int x;
		x = 10000;
		do x--;
		while(x !=0);
	}
}


// With Speed Control
void rotateLeft(int speedOfRotate, int length){
  analogWrite(motor1Pin2, speedOfRotate); //rotates motor
  analogWrite(motor2Pin3, speedOfRotate); //rotates motor
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin4, LOW);    // set the Pin motorPin LOW
  //  delay(length); //waits
    wait(length);
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin3, LOW);    // set the Pin motorPin LOW
}

void rotateRight(int speedOfRotate, int length){
  analogWrite(motor1Pin1, speedOfRotate); //rotates motor
  analogWrite(motor2Pin4, speedOfRotate); //rotates motor
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin3, LOW);    // set the Pin motorPin LOW
  //  delay(length); //waits
    wait(length);
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin4, LOW);    // set the Pin motorPin LOW
}

void forward(int speedOfRotate, int length){
  analogWrite(motor1Pin1, speedOfRotate); //rotates motor
  analogWrite(motor2Pin3, speedOfRotate); //rotates motor
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin4, LOW);    // set the Pin motorPin LOW
//  delay(length); //waits
  wait(length);
  analogWrite(motor1Pin1, 0); //rotates motor
  analogWrite(motor2Pin3, 0);   // set the Pin motorPin LOW
}

void reverse(int speedOfRotate, int length){
  analogWrite(motor1Pin2, speedOfRotate); //rotates motor
  analogWrite(motor2Pin4, speedOfRotate); //rotates motor
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin3, LOW);    // set the Pin motorPin LOW
  //  delay(length); //waits
    wait(length);
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin LOW
  digitalWrite(motor2Pin4, LOW);    // set the Pin motorPin LOW
}


//Full Speed
void rotateForwardFull(int length){
  digitalWrite(motor1Pin1, HIGH); //rotates motor
  digitalWrite(motor2Pin3, HIGH);
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin2 LOW
  digitalWrite(motor2Pin4, LOW);
  delay(length); //waits
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin1 LOW
  digitalWrite(motor2Pin3, LOW);
}

void rotateReverseFull(int length){
  digitalWrite(motor1Pin2, HIGH); //rotates motor
  digitalWrite(motor2Pin4, HIGH);
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin1 LOW
  digitalWrite(motor2Pin3, LOW);
  delay(length); //waits
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin2 LOW
  digitalWrite(motor2Pin4, LOW);
}

void rotateLeftFull(int length){
  digitalWrite(motor1Pin2, HIGH); //rotates motor
  digitalWrite(motor2Pin3, HIGH);
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin1 LOW
  digitalWrite(motor2Pin4, LOW);
  delay(length); //waits
  digitalWrite(motor1Pin2, LOW);    // set the Pin motorPin2 LOW
  digitalWrite(motor2Pin3, LOW);
}

void rotateRightFull(int length){
  digitalWrite(motor1Pin1, HIGH); //rotates motor
  digitalWrite(motor2Pin4, HIGH);
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin1 LOW
  digitalWrite(motor2Pin3, LOW);
  delay(length); //waits
  digitalWrite(motor1Pin1, LOW);    // set the Pin motorPin2 LOW
  digitalWrite(motor2Pin4, LOW);
}


