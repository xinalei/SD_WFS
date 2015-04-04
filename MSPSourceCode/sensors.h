#ifndef SENSORS_H_
#define SENSORS_H_

#ifdef __cplusplus
	extern "C" {
#endif

	int getFwdIR();
	int getRevIR();

#ifdef __cplusplus
	}
#endif

	// not external
	int average_value(int average_count, int irPin);
	void senswait(int delay);

#endif /* SENSORS_H_ */
