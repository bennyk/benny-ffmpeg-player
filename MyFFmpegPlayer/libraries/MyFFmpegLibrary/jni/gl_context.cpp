#include "gl_context.h"

#define LOG_TAG "gl_context.cpp"

#include "gl_shader.hpp"
#include "gl_macros.hpp"
#include "logger.h"

#define GLM_FORCE_RADIANS
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/gtc/type_ptr.hpp"

#include <vector>

using namespace lynda;
using namespace std;

struct vec2 {
  vec2(float _x=0, float _y=0) : x(_x), y(_y) {}
  float x,y;
};

struct vec4 {
  vec4(float _x=0, float _y=0, float _z=0, float _w=0) : x(_x), y(_y), z(_z), w(_w){}
  float x,y,z,w;
};

struct Vertex{
  vec2 position;
  vec2 textureCoordinate;
};

const char * vert = GLSL(100,
  attribute vec4 position;
  attribute vec2 textureCoordinate;              //<-- Texture Coordinate Attribute

  varying vec2 texCoord;                         //<-- To be passed to fragment shader

  uniform mat4 model;
  uniform mat4 view;                 //<-- 4x4 Transformation Matrices
  uniform mat4 projection;

  void main(void){
    texCoord = textureCoordinate;

    gl_Position = projection * view * model * position;
  }

);

const char * frag = GLSL(100,

  uniform sampler2D texture;                       //<-- The texture itself

  varying lowp vec2 texCoord;                           //<-- coordinate passed in from vertex shader

  void main(void){
    gl_FragColor = texture2D( texture, texCoord ); //<-- look up the coordinate's value
  }

);

struct TestRenderer : GlContextRenderer{
    Shader * shader;

    GLuint tID;
    GLuint arrayID;
    GLuint bufferID, elementID;
    GLuint positionID;
    GLuint textureCoordinateID;
    GLuint samplerID;

    //ID of Uniforms
    GLuint modelID, viewID, projectionID;

    TestRenderer() : GlContextRenderer() {}

    virtual bool initialize(){
  	  LOG_INFO("init TestRenderer");

      /*-----------------------------------------------------------------------------
       *  A slab is just a rectangle with texture coordinates
       *-----------------------------------------------------------------------------*/
       //                  position      texture coord
        Vertex slab[] = {
                          {vec2(-.8,-.8), vec2(0,0)}, //bottom-left
                          {vec2(-.8, .8), vec2(0,1)}, //top-left
                          {vec2( .8, .8), vec2(1,1)}, //top-right
                          {vec2( .8,-.8), vec2(1,0)}  //bottom-right
                        };

        GLubyte indices[] = {0,1,2, // first triangle (bottom left - top left - top right)
                             0,2,3}; // second triangle (bottom left - top right - bottom right)

        /*-----------------------------------------------------------------------------
         *  Make some rgba data (can also load a file here)
         *-----------------------------------------------------------------------------*/
        const int tw = 40;
        const int th = 40;
        vector<vec4> data;

        bool checker = false;
        for (int i=0;i<tw;++i){
          float tu = (float)i/tw;
          for (int j=0;j<th;++j){
            float tv = (float)j/th;
            data.push_back( vec4(tu,0,tv,checker) );
            checker = !checker;
          }
          checker = !checker;
        }

        /*-----------------------------------------------------------------------------
         *  Create Shader
         *-----------------------------------------------------------------------------*/
        shader = new Shader(vert,frag);

        /*-----------------------------------------------------------------------------
         *  Get Attribute Locations
         *-----------------------------------------------------------------------------*/
        positionID = glGetAttribLocation( shader->id(), "position" );
        textureCoordinateID = glGetAttribLocation( shader->id(), "textureCoordinate");

        // Get uniform locations
        modelID = glGetUniformLocation(shader -> id(), "model");
        viewID = glGetUniformLocation(shader -> id(), "view");
        projectionID = glGetUniformLocation(shader -> id(), "projection");

        /*-----------------------------------------------------------------------------
         *  Generate And Bind Vertex Array Object
         *-----------------------------------------------------------------------------*/
        GENVERTEXARRAYS(1,&arrayID);
        BINDVERTEXARRAY(arrayID);

        /*-----------------------------------------------------------------------------
         *  Generate Vertex Buffer Object
         *-----------------------------------------------------------------------------*/
        glGenBuffers(1, &bufferID);
        glBindBuffer( GL_ARRAY_BUFFER, bufferID);
        glBufferData( GL_ARRAY_BUFFER,  4 * sizeof(Vertex), slab, GL_STATIC_DRAW );

        /*-----------------------------------------------------------------------------
        *  CREATE THE ELEMENT ARRAY BUFFER OBJECT
        *-----------------------------------------------------------------------------*/
       glGenBuffers(1, &elementID);
       glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, elementID);
       glBufferData( GL_ELEMENT_ARRAY_BUFFER, 6 * sizeof(GLubyte), indices, GL_STATIC_DRAW );

        /*-----------------------------------------------------------------------------
         *  Enable Vertex Attributes and Point to them
         *-----------------------------------------------------------------------------*/
        glEnableVertexAttribArray(positionID);
        glEnableVertexAttribArray(textureCoordinateID);
        glVertexAttribPointer( positionID, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), 0 );

        glVertexAttribPointer( textureCoordinateID,
                               2,
                               GL_FLOAT,
                               GL_FALSE,
                               sizeof(Vertex),
                               (void*) sizeof(vec2) );

        /*-----------------------------------------------------------------------------
         *  Unbind Vertex Array Object and the Vertex Array Buffer
         *-----------------------------------------------------------------------------*/
        BINDVERTEXARRAY(0);
        glBindBuffer( GL_ARRAY_BUFFER, 0 );

        /*-----------------------------------------------------------------------------
         *  Generate Texture and Bind it
         *-----------------------------------------------------------------------------*/
        glGenTextures(1, &tID);
        glBindTexture(GL_TEXTURE_2D, tID);

        /*-----------------------------------------------------------------------------
         *  Allocate Memory on the GPU
         *-----------------------------------------------------------------------------*/
         // target | lod | internal_format | width | height | border | format | type | data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA, GL_FLOAT, NULL);

        /*-----------------------------------------------------------------------------
         *  Load data onto GPU
         *-----------------------------------------------------------------------------*/
        // target | lod | xoffset | yoffset | width | height | format | type | data
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, tw, th, GL_RGBA, GL_FLOAT, &(data[0]) );

        //Mipmaps are good -- the regenerate the texture at various scales
        // and are necessary to avoid black screen if texParameters below are not set
        glGenerateMipmap(GL_TEXTURE_2D);

        // Set these parameters to avoid a black screen
        // caused by improperly mipmapped textures
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        /*-----------------------------------------------------------------------------
         *  Unbind texture
         *-----------------------------------------------------------------------------*/
        glBindTexture(GL_TEXTURE_2D, 0);

        return true;
    }

    virtual void onDraw(){
    	LOG_INFO("XXX enter onDraw");

    	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    	glUseProgram( shader->id() );          //<-- 1. Bind Shader
    	glBindTexture( GL_TEXTURE_2D, tID );   //<-- 2. Bind Texture

    	BINDVERTEXARRAY(arrayID);              //<-- 3. Bind VAO

    	glm::quat q = glm::angleAxis(0.0f, glm::vec3(0,1,0)) * glm::angleAxis(0.0f, glm::vec3(1,0,0));
    	glm::vec3 forwardDir = q * glm::vec3(0,0,-1);

    	glm::quat q1 = glm::angleAxis(0.0f, glm::vec3(0,0,1));

    	// adjust the horizontal distance for IPD too.
    	glm::vec3 eyePos = glm::vec3(0,0,1);
    	glm::mat4 view = glm::lookAt( eyePos, eyePos+forwardDir, q1 * glm::vec3(0,1,0) );

    	glm::mat4 proj = glm::perspective( 3.14f / 3.f, 1.777f, 0.1f,-10.f);

    	glUniformMatrix4fv( viewID, 1, GL_FALSE, glm::value_ptr(view) );
    	glUniformMatrix4fv( projectionID, 1, GL_FALSE, glm::value_ptr(proj) );

    	glm::mat4 model;
    	glUniformMatrix4fv( modelID, 1, GL_FALSE, glm::value_ptr(model) );

    	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);
    	//      glDrawArrays( GL_QUADS, 0, 4);         //<-- 4. Draw the four slab vertices
    	BINDVERTEXARRAY(0);                    //<-- 5. Unbind the VAO

    	glBindTexture( GL_TEXTURE_2D, 0);      //<-- 6. Unbind the texture
    	glUseProgram( 0 );                     //<-- 7. Unbind the shader

    }

};


GlContext::GlContext()
: _window(0), _display(0), _surface(0), _context(0), _renderer(0)
{
	_renderer = new TestRenderer();
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
    EGLint width;
    EGLint height;
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

    if (!eglQuerySurface(display, surface, EGL_WIDTH, &width) ||
        !eglQuerySurface(display, surface, EGL_HEIGHT, &height)) {
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

    LOG_INFO("set viewport width %d height %d", width, height);
    glViewport(0, 0, width, height);
    glEnable(GL_SCISSOR_TEST);

//        ratio = (GLfloat) width / height;
//        glMatrixMode(GL_PROJECTION);
//        glLoadIdentity();
//        glFrustumf(-ratio, ratio, -1, 1, 2, 10);

    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LESS);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    /*
    if (stereoMode) {
        // setup stereoscopic mode
    	_leftChannel = ParcelInfo(0, 0, width/2, height, LEFT_CHANNEL);
    	_rightChannel = ParcelInfo(width/2, 0, width/2, height, RIGHT_CHANNEL);
    } else {
    	// only _leftChannel is rendered but we defined the right anyway.
    	_leftChannel = ParcelInfo(0, 0, width, height, SINGLE_CHANNEL);
    	_rightChannel = ParcelInfo(0, 0, width, height, SINGLE_CHANNEL);
    }
    */

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
	_renderer->onDraw();
}

bool GlContext::swapBuffer() {
	return eglSwapBuffers(_display, _surface);
}

GlContext *glcontext_initialize(ANativeWindow *window)
{
	GlContext *aobj = new GlContext();
	if (aobj->initialize(window)) {
		return aobj;
	}
	return NULL;
}

void glcontext_draw_frame(GlContext *context,
		const uint8_t *src, int width, int height)
{
	LOG_INFO("draw frame with %dx%d", width, height);
	context->draw();
}

int glcontext_swapBuffer(GlContext *context)
{
	return !context->swapBuffer() ? -1 : 0;
}
