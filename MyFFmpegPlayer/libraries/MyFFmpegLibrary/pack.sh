#!/bin/sh

if [ ! -d "lib" ]; then
	# create a link to libs if non exists
	ln -s libs lib
fi

ndk-build NDK_DEBUG=1
zip -r libffmpeg-jni.jar lib

# making sure the destination path locates the libs directory in the app for the jar file.
mv libffmpeg-jni.jar ../../app/libs/libffmpeg-jni.jar

