#include "gl_context.h"

#define LOG_TAG "gl_context.cpp"

#include "TestRenderer.hpp"
#include "FrameRenderer.hpp"

GlContext::GlContext(int frameWidth, int frameHeight)
: _window(0), _display(0), _surface(0), _context(0), _renderer(0), _width(0), _height(0), stereoMode(true)
{
	_renderer = new framerenderer::FrameRenderer(this, frameWidth, frameHeight);
//	_renderer = new testrenderer::TestRenderer(this);
}

GlContext::~GlContext()
{}

bool GlContext::initialize(ANativeWindow *window)
{
    EGLDisplay display;
    EGLConfig config;
    EGLint numConfigs;
    EGLint format;
    EGLSurface surface;
    EGLContext context;
    GLfloat ratio;

    LOG_INFO("Initializing GL context");

    if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOG_ERROR("eglGetDisplay() returned error %d", eglGetError());
        return false;
    }
    if (!eglInitialize(display, 0, 0)) {
        LOG_ERROR("eglInitialize() returned error %d", eglGetError());
        return false;
    }

    const EGLint RGBX_8888_ATTRIBS[] =
    {
    		EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
			EGL_BLUE_SIZE, 8,
			EGL_GREEN_SIZE, 8,
			EGL_RED_SIZE, 8,
			EGL_DEPTH_SIZE, 8,
			EGL_NONE
    };

    const EGLint RGB_565_ATTRIBS[] =
    {
    		EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
			EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
			EGL_BLUE_SIZE, 5,
			EGL_GREEN_SIZE, 6,
			EGL_RED_SIZE, 5,
			EGL_DEPTH_SIZE, 8, EGL_NONE
    };

    const EGLint* attribList;
    int windowFormat = ANativeWindow_getFormat(window);
    if (true || windowFormat == WINDOW_FORMAT_RGBA_8888 || windowFormat == WINDOW_FORMAT_RGBX_8888) {
    	LOG_INFO("setting window format to WINDOW_FORMAT_RGBA_8888");
    	attribList = RGBX_8888_ATTRIBS;
    }
    else {
    	LOG_INFO("setting window format to WINDOW_FORMAT_RGB_565");
    	attribList = RGB_565_ATTRIBS;
    }

    if (!eglChooseConfig(display, attribList, &config, 1, &numConfigs)) {
        LOG_ERROR("eglChooseConfig() returned error %d", eglGetError());
        destroy();
        return false;
    }

    if (!eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format)) {
        LOG_ERROR("eglGetConfigAttrib() returned error %d", eglGetError());
        destroy();
        return false;
    }

    ANativeWindow_setBuffersGeometry(window, 0, 0, format);

    if ((surface = eglCreateWindowSurface(display, config, window, 0)) == EGL_NO_SURFACE) {
        LOG_ERROR("eglCreateWindowSurface() returned error %d", eglGetError());
        destroy();
        return false;
    }

    EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
//        EGLint *contextAttribs = NULL;
    if ((context = eglCreateContext(display, config, 0, contextAttribs)) == EGL_NO_CONTEXT) {
        LOG_ERROR("eglCreateContext() returned error %d", eglGetError());
        destroy();
        return false;
    }

    if (!eglMakeCurrent(display, surface, surface, context)) {
        LOG_ERROR("eglMakeCurrent() returned error %d", eglGetError());
        destroy();
        return false;
    }

    if (!eglQuerySurface(display, surface, EGL_WIDTH, &_width) ||
        !eglQuerySurface(display, surface, EGL_HEIGHT, &_height)) {
        LOG_ERROR("eglQuerySurface() returned error %d", eglGetError());
        destroy();
        return false;
    }

    _display = display;
    _surface = surface;
    _context = context;
    _window = window;

//        glDisable(GL_DITHER);
//        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        glEnable(GL_CULL_FACE);
//        glShadeModel(GL_SMOOTH);
//        glEnable(GL_DEPTH_TEST);

    LOG_INFO("set viewport width %d height %d", _width, _height);
    glViewport(0, 0, _width, _height);
    glEnable(GL_SCISSOR_TEST);

//        ratio = (GLfloat) width / height;
//        glMatrixMode(GL_PROJECTION);
//        glLoadIdentity();
//        glFrustumf(-ratio, ratio, -1, 1, 2, 10);

    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LESS);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glEnable(GL_TEXTURE_2D);


    if (stereoMode) {
        // setup stereoscopic mode
    	_leftChannel = ParcelInfo(0, 0, _width/2, _height, LEFT_CHANNEL);
    	_rightChannel = ParcelInfo(_width/2, 0, _width/2, _height, RIGHT_CHANNEL);
    } else {
    	// only _leftChannel is rendered but we defined the right anyway.
    	_leftChannel = ParcelInfo(0, 0, _width, _height, SINGLE_CHANNEL);
    	_rightChannel = ParcelInfo(0, 0, _width, _height, SINGLE_CHANNEL);
    }

    LOG_INFO("Version: %s GLSL: %s", glGetString(GL_VERSION), glGetString(GL_SHADING_LANGUAGE_VERSION));
    return _renderer->initialize();
}

void GlContext::destroy() {
    LOG_INFO("Destroying context");

    eglMakeCurrent(_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(_display, _context);
    eglDestroySurface(_display, _surface);
    eglTerminate(_display);

    _display = EGL_NO_DISPLAY;
    _surface = EGL_NO_SURFACE;
    _context = EGL_NO_CONTEXT;

    if (_window != NULL) {
    	ANativeWindow_release(_window);
    }

    return;
}

void GlContext::draw() {

    glViewport(_leftChannel.x, _leftChannel.y, _leftChannel.width, _leftChannel.height);
    glScissor(_leftChannel.x, _leftChannel.y, _leftChannel.width, _leftChannel.height);
    _renderer->onDraw(_leftChannel);

    if (stereoMode) {
    	glViewport(_rightChannel.x, _rightChannel.y, _rightChannel.width, _rightChannel.height);
    	glScissor(_rightChannel.x, _rightChannel.y, _rightChannel.width, _rightChannel.height);
    	_renderer->onDraw(_rightChannel);
    }
}

void GlContext::bindRGBA(const uint8_t *src, int width, int height) {
	_renderer->bindRGBA(src, width, height);
}

bool GlContext::swapBuffer() {
	return eglSwapBuffers(_display, _surface);
}

float GlContext::aspectRatio()
{
	return (float)_width/_height;
}

void GlContext::setLookatAngles(float azimuth, float pitch, float roll)
{
	_lookatAngles[0] = azimuth;
	_lookatAngles[1] = pitch;
	_lookatAngles[2] = roll;
}

void GlContext::getLookatAngles(float &azimuth, float &pitch, float &roll)
{
	azimuth = _lookatAngles[0];
	pitch = _lookatAngles[1];
	roll = _lookatAngles[2];
}

void GlContext::setIPDDistancePx(unsigned ipdPx)
{
	_leftChannel.halfIPDDistancePx = ipdPx/2;
	_rightChannel.halfIPDDistancePx = ipdPx/2;
}

GlContext *glcontext_initialize(ANativeWindow *window, int frameWidth, int frameHeight)
{
	GlContext *aobj = new GlContext(frameWidth, frameHeight);
	if (aobj->initialize(window)) {
		return aobj;
	}
	return NULL;
}

void glcontext_draw_frame(GlContext *context,
		const uint8_t *src, int width, int height)
{
//	LOG_INFO("draw frame src %x with %dx%d", src, width, height);
	context->bindRGBA(src, width, height);
	context->draw();
}

int glcontext_swapBuffer(GlContext *context)
{
	return !context->swapBuffer() ? -1 : 0;
}

void glcontext_setLookatAngles(GlContext *context, float azimuth, float pitch, float roll)
{
	context->setLookatAngles(azimuth, pitch, roll);
}

void glcontext_setIPDDistancePx(GlContext *context, unsigned ipdPx)
{
	context->setIPDDistancePx(ipdPx);
}
