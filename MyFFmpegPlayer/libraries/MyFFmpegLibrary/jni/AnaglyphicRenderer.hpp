#include "gl_shader.hpp"
#include "gl_macros.hpp"
#include "logger.h"

#define GLM_FORCE_RADIANS
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/gtc/type_ptr.hpp"

#include <vector>

namespace anaglyphicrenderer {

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

  uniform sampler2D texture;                        //<-- The texture itself
  uniform bool renderLeftFrame;

  varying lowp vec2 texCoord;                            //<-- coordinate passed in from vertex shader

  void main(void){
      lowp vec4 texColor = texture2D( texture, texCoord );
      if (renderLeftFrame) {
      	  gl_FragColor = vec4(texColor.r, 0.0, 0.0, 1.0);
  	  } else {
  		  gl_FragColor = vec4(0.0, texColor.gb, 1.0);
  	  }
  }

);

struct AnaglyphicRenderer : GlContextRenderer{
    Shader * shader;

    GLuint tID = 0;
    GLuint arrayID;
    GLuint bufferID, elementID;
    GLuint positionID;
    GLuint textureCoordinateID;
    GLuint samplerID;

    //ID of Uniforms
    GLuint modelID, viewID, projectionID, renderLeftFrameID;

    GLuint frameWidth, frameHeight;

    AnaglyphicRenderer(GlContext *context, int fWidth, int fHeight)
    : GlContextRenderer(context), frameWidth(fWidth), frameHeight(fHeight)
    {
    }

    virtual bool initialize(){
  	  LOG_INFO("init FrameRenderer");

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

        GLubyte indices[] = {0,1,2, // first triangle (bottom left - top left - top right)
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
        renderLeftFrameID = glGetUniformLocation(shader -> id(), "renderLeftFrame");

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
         *  Generate Texture and Bind it
         *-----------------------------------------------------------------------------*/
//        glEnable(GL_TEXTURE_2D);
        glGenTextures(1, &tID);
        glBindTexture(GL_TEXTURE_2D, tID);

        /*-----------------------------------------------------------------------------
         *  Allocate Memory on the GPU
         *-----------------------------------------------------------------------------*/
         // target | lod | internal_format | width | height | border | format | type | data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, frameWidth, frameHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);

        /*-----------------------------------------------------------------------------
         *  Unbind Vertex Array Object and the Vertex Array Buffer
         *-----------------------------------------------------------------------------*/
        BINDVERTEXARRAY(0);
        glBindBuffer( GL_ARRAY_BUFFER, 0 );

        return true;
    }

    virtual void bindRGBA(const uint8_t *data, int width, int height){

        glBindTexture(GL_TEXTURE_2D, tID);

        /*-----------------------------------------------------------------------------
         *  Load data onto GPU
         *-----------------------------------------------------------------------------*/
        // target | lod | xoffset | yoffset | width | height | format | type | data
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data );

        //mipmaps generation is slow and can cause drastic reduction of frame rate down to sub 10fps.

        //Mipmaps are good -- the regenerate the texture at various scales
        // and are necessary to avoid black screen if texParameters below are not set
//    	glHint(GL_GENERATE_MIPMAP_HINT, GL_FASTEST);
//        glGenerateMipmap(GL_TEXTURE_2D);

        // Set these parameters to avoid a black screen
        // caused by improperly mipmapped textures
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        /*-----------------------------------------------------------------------------
         *  Unbind texture
         *-----------------------------------------------------------------------------*/

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    virtual void onDraw(ParcelInfo channelInfo){
    	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    	glUseProgram( shader->id() );          //<-- 1. Bind Shader
    	glBindTexture( GL_TEXTURE_2D, tID );   //<-- 2. Bind Texture

    	BINDVERTEXARRAY(arrayID);              //<-- 3. Bind VAO

    	// setup mvp matrix

    	float azimuth, pitch, roll;
    	_context->getLookatAngles(azimuth, pitch, roll);

    	glm::quat q = glm::angleAxis(azimuth, glm::vec3(0,1,0)) * glm::angleAxis(roll, glm::vec3(1,0,0));
    	glm::vec3 forwardDir = q * glm::vec3(0,0,-1);

    	glm::quat q1 = glm::angleAxis(pitch, glm::vec3(0,0,1));

    	// adjust the horizontal distance for IPD too.
//    	LOG_INFO("ipd ratio %.2f", channelInfo.getHalfIPDOffsetRatio());
    	glm::vec3 eyePos = glm::vec3(channelInfo.getHalfIPDOffsetRatio(),0,1.7);
    	glm::mat4 view = glm::lookAt( eyePos, eyePos+forwardDir, q1 * glm::vec3(0,1,0) );

    	glm::mat4 proj = glm::perspective( 3.14f / 3.f, channelInfo.aspectRatio(), 1.f,10.f);

    	glUniformMatrix4fv( viewID, 1, GL_FALSE, glm::value_ptr(view) );
    	glUniformMatrix4fv( projectionID, 1, GL_FALSE, glm::value_ptr(proj) );

    	glm::mat4 model;
    	glUniformMatrix4fv( modelID, 1, GL_FALSE, glm::value_ptr(model) );

    	if (channelInfo.tag == LEFT_CHANNEL) {
    		glUniform1i(renderLeftFrameID, true);
    	} else {
    		glUniform1i(renderLeftFrameID, false);
    	}

    	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, 0);  //<-- 4. Draw the four slab vertices
    	BINDVERTEXARRAY(0);                    //<-- 5. Unbind the VAO

    	glBindTexture( GL_TEXTURE_2D, 0);      //<-- 6. Unbind the texture
    	glUseProgram( 0 );                     //<-- 7. Unbind the shader

    }

};

}

