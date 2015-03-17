#include <android/native_window.h>
#include <EGL/egl.h> // requires ndk r5 or newer
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifdef __cplusplus

class GlContext;

struct GlContextRenderer
{
	GlContext *_context;
	GlContextRenderer(GlContext *context) : _context(context)
	{
	}

	virtual bool initialize() = 0;
	virtual void onDraw() = 0;
	virtual void bindRGBA(const uint8_t *data, int width, int height) = 0;
};

class GlContext
{
public:

	GlContext(int frameWidth, int frameHeight);
	~GlContext();

public:
	bool initialize(ANativeWindow *window);
	void draw();
	bool swapBuffer();
	void bindRGBA(const uint8_t *data, int width, int height);
	float aspectRatio();
	void setLookatAngles(float azimuth, float pitch, float roll);
	void getLookatAngles(float &azimuth, float &pitch, float &roll);

private:
	void destroy();

private:
    ANativeWindow* _window;

    EGLDisplay _display;
    EGLSurface _surface;
    EGLContext _context;

    GlContextRenderer *_renderer;
    int _width, _height;

    // lookAt angles in Euler format.
    float _lookatAngles[3];
};

#else

// The C interface
typedef struct GlContextHandle GlContext;

#endif

#ifdef __cplusplus
extern "C" {
#endif

GlContext *glcontext_initialize(ANativeWindow *window, int frameWidth, int frameHeight);
void glcontext_draw_frame(GlContext *context,
		const uint8_t *src, int width, int height);
int glcontext_swapBuffer(GlContext *context);
void glcontext_setLookatAngles(GlContext *context, float azimuth, float pitch, float roll);

#ifdef __cplusplus
}
#endif
