
LOCAL_PATH := $(call my-dir)
MY_SYSROOT := ../../../android-ffmpeg-x264/Project/jni/out/$(TARGET_ARCH_ABI)

include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg-jni

LOCAL_C_INCLUDES += $(MY_SYSROOT)/include

# ffmpeg private header files
LOCAL_C_INCLUDES += ../../../android-ffmpeg-x264/Project/jni/ffmpeg

# opengl helpers
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(LOCAL_PATH)/glm

#LOCAL_LDLIBS += -L$(MY_SYSROOT)/lib -lavcodec -lavformat -lavresample -lavutil -lswresample
LOCAL_LDLIBS += -L$(MY_SYSROOT)/lib -lavdevice -lavfilter -lavformat -lswscale -lpostproc -lswresample -lavcodec -lavutil -lx264 -lrtmp -lssl -lcrypto

LOCAL_LDLIBS += -landroid
LOCAL_LDLIBS += -llog -ljnigraphics -lz
LOCAL_LDLIBS += -lEGL -lGLESv2

LOCAL_SRC_FILES:= ffmpeg-jni.c fps.c player.c queue.c helpers.c convert.cpp jni-protocol.c gl_context.cpp
LOCAL_PRELINK_MODULE := false

LOCAL_CFLAGS += -DLIBYUV -DGL_GLEXT_PROTOTYPES -fexceptions -D__STDC_CONSTANT_MACROS
#-DMEASURE_TIME
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libyuv/include
LOCAL_CPP_INCLUDES += $(LOCAL_PATH)/libyuv/include
LOCAL_STATIC_LIBRARIES += libyuv_static
LOCAL_REQUIRED_MODULES += libyuv_static

include $(BUILD_SHARED_LIBRARY)

#nativetester-jni library
include $(CLEAR_VARS)

ifdef FEATURE_VFPV3
LOCAL_CFLAGS += -DFEATURE_VFPV3
endif

ifdef FEATURE_NEON
LOCAL_CFLAGS += -DFEATURE_NEON
endif

LOCAL_ALLOW_UNDEFINED_SYMBOLS=false
LOCAL_MODULE := nativetester-jni
LOCAL_SRC_FILES := nativetester-jni.c nativetester.c
LOCAL_STATIC_LIBRARIES := cpufeatures
LOCAL_LDLIBS  := -llog
include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))

