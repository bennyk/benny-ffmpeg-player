#include <android/native_window.h>
#include <EGL/egl.h> // requires ndk r5 or newer
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifdef __cplusplus

struct GlContextRenderer
{
	virtual bool initialize() = 0;
	virtual void onDraw() = 0;
	virtual void bindRGBA(const uint8_t *data, int width, int height) = 0;
};

class GlContext
{
public:

	GlContext();
	~GlContext();

public:
	bool initialize(ANativeWindow *window);
	void draw();
	bool swapBuffer();
	void bindRGBA(const uint8_t *data, int width, int height);

private:
	void destroy();


private:
    ANativeWindow* _window;

    EGLDisplay _display;
    EGLSurface _surface;
    EGLContext _context;

    GlContextRenderer *_renderer;
};

#else

// The C interface
typedef struct GlContextHandle GlContext;

#endif

#ifdef __cplusplus
extern "C" {
#endif

GlContext *glcontext_initialize(ANativeWindow *window);
void glcontext_draw_frame(GlContext *context,
		const uint8_t *src, int width, int height);
int glcontext_swapBuffer(GlContext *context);

#ifdef __cplusplus
}
#endif
