#ifndef SENSORS_H_
#define SENSORS_H_

#ifdef __cplusplus
	extern "C" {
#endif

	int getFwd1IR();
	int getFwd2IR();
	int getRev1IR();
	int getRev2IR();

#ifdef __cplusplus
	}
#endif

	// not external
	int average_value(int average_count, int irPin);
	void senswait(int delay);

#endif /* SENSORS_H_ */
