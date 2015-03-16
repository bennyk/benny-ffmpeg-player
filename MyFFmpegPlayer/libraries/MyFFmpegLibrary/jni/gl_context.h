#include <android/native_window.h>
#include <EGL/egl.h> // requires ndk r5 or newer
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#ifdef __cplusplus

class GlContext
{
public:

	GlContext();
	~GlContext();

public:
	bool initialize(ANativeWindow *window);

private:
	void destroy();


private:
    ANativeWindow* _window;

    EGLDisplay _display;
    EGLSurface _surface;
    EGLContext _context;
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

#ifdef __cplusplus
}
#endif
