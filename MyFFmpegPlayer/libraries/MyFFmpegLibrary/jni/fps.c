
#include "fps.h"

#include <time.h>
#include <stdlib.h>
#include <android/log.h>

// desired fps
#define MAX_FPS 50

// maximum number of frames to be skipped
#define MAX_FRAME_SKIPS 5

// the frame period
#define FRAME_PERIOD 1000 / MAX_FPS;

// we'll be reading the stats every second
#define STAT_INTERVAL 1000000000LL //ns


#define LOG_LEVEL 99
#define LOG_TAG "fps.c"
#define LOGI(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(level, ...) if (level <= LOG_LEVEL + 10) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}
#define LOGW(level, ...) if (level <= LOG_LEVEL + 5) {__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);}

fpsCounter *allocFpsCounter() {
	fpsCounter *counter = malloc(sizeof(fpsCounter));
	memset(counter, 0, sizeof(fpsCounter));
	LOGI(9, "init lastStatusStore %lld", counter->lastStatusStore);

	return counter;
}

void destroyFpsCounter(fpsCounter **counter) {
	free(*counter);
	*counter = NULL;
}

static int64_t getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
}

static double computeRunningAverageFps(fpsCounter *counter, double newFps)
{
	int lastFpsIndex = counter->statsCount % MAX_FPS_SAMPLES;
    counter->fpsRunningSum -= counter->fpsStore[lastFpsIndex];
    counter->fpsRunningSum += newFps;
    counter->fpsStore[lastFpsIndex] = newFps;
    counter->statsCount++;

    /* return average */
    return (counter->statsCount >= MAX_FPS_SAMPLES) ?
    	((double)counter->fpsRunningSum/MAX_FPS_SAMPLES)
		: ((double)counter->fpsRunningSum/(counter->statsCount));
}

void computeFps(fpsCounter *counter) {
	counter->frameCountPerStatCycle++;
	counter->totalFrameCount++;

	// check the actual time
	counter->statusIntervalTimer += (getTimeNsec() - counter->statusIntervalTimer);
//	LOGI(9, "statusIntervalTimer %lld lastStatusStore %lld", counter->statusIntervalTimer, counter->lastStatusStore);

	if (counter->statusIntervalTimer >= counter->lastStatusStore + STAT_INTERVAL) {

		// calculate the actual frames pers status check interval
		double actualFps = (double)(counter->frameCountPerStatCycle) / (STAT_INTERVAL / 1000000000LL);

		double avgFps = computeRunningAverageFps(counter, actualFps);

		// saving the number of total frames skipped
		counter->totalFramesSkipped += counter->framesSkippedPerStatCycle;

		// resetting the counters after a status record (1 sec)
		counter->framesSkippedPerStatCycle = 0;
		counter->statusIntervalTimer = 0;
		counter->frameCountPerStatCycle = 0;

		counter->statusIntervalTimer = getTimeNsec();

		counter->lastStatusStore = counter->statusIntervalTimer;
//          Log.d(TAG, "Average FPS:" + df.format(averageFps));
		LOGI(1, "Average FPS: %.2f Actual FPS: %.2f", avgFps, actualFps);
	}
}
