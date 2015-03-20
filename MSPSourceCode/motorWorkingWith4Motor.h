/*
 * motorWorkingWith4Motor.h
 *
 *  Created on: Mar 20, 2015
 *      Author: Jimmy
 */

#ifndef MOTORWORKINGWITH4MOTOR_H_
#define MOTORWORKINGWITH4MOTOR_H_

#ifdef __cplusplus
	extern "C" {
#endif

	void setup();
	void rotateLeft(int speedOfRotate, int length);
	void rotateRight(int speedOfRotate, int length);
	void forward(int speedOfRotate, int length);
	void reverse(int speedOfRotate, int length);
	void rotateForwardFull(int length);
	void rotateReverseFull(int length);
	void rotateLeftFull(int length);
	void rotateRightFull(int length);


#ifdef __cplusplus
	}
#endif


#endif /* MOTORWORKINGWITH4MOTOR_H_ */
