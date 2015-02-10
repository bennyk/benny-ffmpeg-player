
#ifndef H_FPS
#define H_FPS

#include <stdint.h>

// the average will be calculated by storing
// the last n FPSs
#define MAX_FPS_SAMPLES 100

typedef struct {

	// last time the status was stored
	int64_t lastStatusStore;

	// the status time counter
	int64_t statusIntervalTimer;

	// number of frames skipped since the game started
	long totalFramesSkipped;

	// number of frames skipped in a store cycle (1 sec)
	long framesSkippedPerStatCycle;

	// number of rendered frames in an interval
	int frameCountPerStatCycle;
	long totalFrameCount;

	// the last FPS values
	double fpsStore[MAX_FPS_SAMPLES];

	double fpsRunningSum;

	// the number of times the stat has been read
	long statsCount;

	// the average FPS since the game started
	double averageFps;

} fpsCounter;

fpsCounter *allocFpsCounter();
void destroyFpsCounter(fpsCounter **clock);
void computeFps(fpsCounter *clock);

#endif
