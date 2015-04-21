#include "gl_shader.hpp"
#include "gl_macros.hpp"
#include "logger.h"

#define GLM_FORCE_RADIANS
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/gtc/type_ptr.hpp"

#include <vector>

namespace yuvshader {

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

// YUV420 -> RGB shader based on work flawlessly.
// https://gist.github.com/roxlu/9329339
precision highp float;
varying lowp vec2 texCoord;                           //<-- coordinate passed in from vertex shader

uniform sampler2D y_tex;
uniform sampler2D u_tex;
uniform sampler2D v_tex;

const vec3 R_cf = vec3(1.164383,  0.000000,  1.596027);
const vec3 G_cf = vec3(1.164383, -0.391762, -0.812968);
const vec3 B_cf = vec3(1.164383,  2.017232,  0.000000);
const vec3 offset = vec3(-0.0625, -0.5, -0.5);

void main() {
	float y = texture2D(y_tex, texCoord).r;
	float u = texture2D(u_tex, texCoord).r;
	float v = texture2D(v_tex, texCoord).r;

#if 1
	vec3 yuv = vec3(y,u,v);
	yuv += offset;
	gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
	gl_FragColor.r = dot(yuv, R_cf);
	gl_FragColor.g = dot(yuv, G_cf);
	gl_FragColor.b = dot(yuv, B_cf);
#else
	// similar glsl shader based on below link which work out to the same numbers has been tested too.
	// http://www.fourcc.org/source/YUV420P-OpenGL-GLSLang.c

	y=1.1643*(y-0.0625);
	u=u-0.5;
	v=v-0.5;

	float r=y+1.5958*v;
	float g=y-0.39173*u-0.81290*v;
	float b=y+2.017*u;

	gl_FragColor=vec4(r,g,b,1.0);
#endif

}
);

struct YUVShader : GlContextRenderer{
	Shader * shader;

	GLuint y_texID, u_texID, v_texID;
	GLuint arrayID;
	GLuint bufferID, elementID;
	GLuint positionID;
	GLuint textureCoordinateID;
	GLuint samplerID;

	//ID of Uniforms
	GLuint modelID, viewID, projectionID;

	GLuint frameWidth, frameHeight;

	YUVShader(GlContext *context, int fWidth, int fHeight)
	: GlContextRenderer(context), frameWidth(fWidth), frameHeight(fHeight)
	{
	}

	virtual bool initialize(){
		LOG_INFO("init YUVShader with %dx%d", frameWidth, frameHeight);

		/*-----------------------------------------------------------------------------
		 *  A slab is just a rectangle with texture coordinates
		 *-----------------------------------------------------------------------------*/
		//                  position      texture coord

		float frameRatio = (float)frameWidth/frameHeight;
		Vertex slab[] = {
				{vec2(-frameRatio,-1), vec2(0,1)}, //bottom-left
				{vec2(-frameRatio, 1), vec2(0,0)}, //top-left
				{vec2( frameRatio, 1), vec2(1,0)}, //top-right
				{vec2( frameRatio,-1), vec2(1,1)}  //bottom-right
		};

		GLubyte indices[] = {
				0,1,2,  // first triangle (bottom left - top left - top right)
				0,2,3}; // second triangle (bottom left - top right - bottom right)

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

		// assign texture units
		glUniform1i(glGetUniformLocation(shader -> id(), "y_tex"), 0);
		glUniform1i(glGetUniformLocation(shader -> id(), "u_tex"), 1);
		glUniform1i(glGetUniformLocation(shader -> id(), "v_tex"), 2);

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

		generateYUVTextures(frameWidth, frameHeight);


		/*-----------------------------------------------------------------------------
		 *  Unbind Vertex Array Object and the Vertex Array Buffer
		 *-----------------------------------------------------------------------------*/
		BINDVERTEXARRAY(0);
		glBindBuffer( GL_ARRAY_BUFFER, 0 );

		return true;
	}

	void generateYUVTextures(int width, int height) {
		/*-----------------------------------------------------------------------------
		 *  Generate Texture and Bind it
		 *-----------------------------------------------------------------------------*/
		glEnable(GL_TEXTURE_2D);
		glGenTextures(1, &y_texID);
		glBindTexture(GL_TEXTURE_2D, y_texID);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		glGenTextures(1, &u_texID);
		glBindTexture(GL_TEXTURE_2D, u_texID);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width/2, height/2, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		glGenTextures(1, &v_texID);
		glBindTexture(GL_TEXTURE_2D, v_texID);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width/2, height/2, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, NULL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
	}

	virtual bool bindFrame(AVFrame *frame) {
//		LOG_INFO("linesize %d %d %d %dx%d", frame->linesize[0], frame->linesize[1], frame->linesize[2], frame->width, frame->height);

		// TODO: weird sample in our case that the frame linesize is reported to be different than the frame width as indicated in AVCodecContext->width hmmm...
		if (frame->linesize[0] != frameWidth) {
			LOG_WARN("frame dimension differ from previous. Regenerating texture buffer");
			generateYUVTextures(frame->linesize[0], frameHeight);
			frameWidth = frame->linesize[0];
		}

		glBindTexture(GL_TEXTURE_2D, y_texID);
//    	  glPixelStorei(GL_UNPACK_ALIGNMENT, frame->linesize[0]);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frame->linesize[0], frameHeight, GL_LUMINANCE, GL_UNSIGNED_BYTE, frame->data[0]);

		glBindTexture(GL_TEXTURE_2D, u_texID);
//    	  glPixelStorei(GL_UNPACK_ROW_LENGTH, frame->linesize[1]);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frame->linesize[1], frameHeight/2, GL_LUMINANCE, GL_UNSIGNED_BYTE, frame->data[1]);

		glBindTexture(GL_TEXTURE_2D, v_texID);
//    	  glPixelStorei(GL_UNPACK_ROW_LENGTH, frame->linesize[2]);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, frame->linesize[2], frameHeight/2, GL_LUMINANCE, GL_UNSIGNED_BYTE, frame->data[2]);

		return true;
	}

	virtual void onDraw(ParcelInfo channelInfo){
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glUseProgram( shader->id() );          //<-- 1. Bind Shader

		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, y_texID);

		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, u_texID);

		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, v_texID);

		BINDVERTEXARRAY(arrayID);              //<-- 3. Bind VAO

		// setup mvp matrix

		float azimuth, pitch, roll;
		_context->getLookatAngles(azimuth, pitch, roll);

		glm::quat q = glm::angleAxis(azimuth, glm::vec3(0,1,0)) * glm::angleAxis(roll, glm::vec3(1,0,0));
		glm::vec3 forwardDir = q * glm::vec3(0,0,-1);

		glm::quat q1 = glm::angleAxis(pitch, glm::vec3(0,0,1));

		// adjust the horizontal distance for IPD too.
		glm::vec3 eyePos = glm::vec3(channelInfo.getHalfIPDOffsetRatio(),0,1.7);
		glm::mat4 view = glm::lookAt( eyePos, eyePos+forwardDir, q1 * glm::vec3(0,1,0) );

		glm::mat4 proj = glm::perspective( 3.14f / 3.f, channelInfo.aspectRatio(), 1.f,10.f);

		glUniformMatrix4fv( viewID, 1, GL_FALSE, glm::value_ptr(view) );
		glUniformMatrix4fv( projectionID, 1, GL_FALSE, glm::value_ptr(proj) );

		glm::mat4 model;
		glUniformMatrix4fv( modelID, 1, GL_FALSE, glm::value_ptr(model) );

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);  //<-- 4. Draw the four slab vertices
		BINDVERTEXARRAY(0);                    //<-- 5. Unbind the VAO

		glBindTexture( GL_TEXTURE_2D, 0);      //<-- 6. Unbind the texture
		glUseProgram( 0 );                     //<-- 7. Unbind the shader

	}

};

}

