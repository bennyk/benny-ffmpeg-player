#include <android/native_window.h>
#include <EGL/egl.h> // requires ndk r5 or newer
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "libavutil/frame.h"
#include "libavutil/pixfmt.h"

#ifdef __cplusplus
}
#endif


//typedef struct _AVFrame AVFrame;

#ifdef __cplusplus

class GlContext;

enum ChannelTag { LEFT_CHANNEL, RIGHT_CHANNEL, SINGLE_CHANNEL };

struct ParcelInfo {
	EGLint x, y, width, height;
	ChannelTag tag;
	unsigned halfIPDDistancePx;

	ParcelInfo() : x(0), y(0), width(0), height(0), tag(SINGLE_CHANNEL), halfIPDDistancePx(0)
	{}

	ParcelInfo(EGLint x, EGLint y, EGLint width, EGLint height, ChannelTag tag)
	: x(x), y(y), width(width), height(height), tag(tag), halfIPDDistancePx(0) {

		// init a safe default
		halfIPDDistancePx = width/2;
	}

	float getHalfIPDOffsetRatio() {
		if (tag == LEFT_CHANNEL)
			return - (float) halfIPDDistancePx / width ;
		else if (tag == RIGHT_CHANNEL)
			return (float) halfIPDDistancePx / width ;
		else
			return 0.0f;
	}

	float aspectRatio() {
		return (float)width/height;
	}

};

struct GlContextRenderer
{
	GlContext *_context;
	GlContextRenderer(GlContext *context) : _context(context)
	{
	}

	virtual bool initialize() = 0;
	virtual void onDraw(ParcelInfo channelInfo) = 0;
	virtual bool bindFrame(AVFrame *frame) = 0;
};

class GlContext
{
public:

	GlContext();
	~GlContext();

public:
	bool initialize(ANativeWindow *window, int frameWidth, int frameHeight, AVPixelFormat pix_fmt);
	void draw();
	int32_t getFormat() { return ANativeWindow_getFormat(_window); }
	bool swapBuffer();
	bool bindFrame(AVFrame *frame);
	float aspectRatio();
	void setLookatAngles(float azimuth, float pitch, float roll);
	void getLookatAngles(float &azimuth, float &pitch, float &roll);
	void setIPDDistancePx(unsigned ipdPx);

private:
	void destroy();

public:
    int _width, _height;

private:
    ANativeWindow* _window;

    EGLDisplay _display;
    EGLSurface _surface;
    EGLContext _context;

    GlContextRenderer *_renderer;

    // lookAt angles in Euler format.
    float _lookatAngles[3];

    ParcelInfo _leftChannel, _rightChannel;
    bool stereoMode;
};

#else

// The C interface
typedef struct GlContextHandle GlContext;

#endif

#ifdef __cplusplus
extern "C" {
#endif

GlContext *glcontext_initialize(ANativeWindow *window, int frameWidth, int frameHeight, enum AVPixelFormat pix_fmt);
void glcontext_draw_frame(GlContext *context, AVFrame *frame);
int glcontext_swapBuffer(GlContext *context);
void glcontext_setLookatAngles(GlContext *context, float azimuth, float pitch, float roll);
void glcontext_setIPDDistancePx(GlContext *context, unsigned ipdPx);

#ifdef __cplusplus
}
#endif
