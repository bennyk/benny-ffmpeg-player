/*
 * helpers.c
 * Copyright (c) 2012 Jacek Marchwicki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <jni.h>
#include <string.h>

#include "helpers.h"

jfieldID java_get_field(JNIEnv *env, char * class_name, JavaField field) {
	jclass clazz = (*env)->FindClass(env, class_name);
	jfieldID jField = (*env)->GetFieldID(env, clazz, field.name, field.signature);
	(*env)->DeleteLocalRef(env, clazz);
	return jField;
}

jmethodID java_get_method(JNIEnv *env, jclass class, JavaMethod method) {
	return (*env)->GetMethodID(env, class, method.name, method.signature);
}

void argb_crop(const uint8_t *src, int src_stride, int width, int height,
		int x, int y, int w, int h, uint8_t *dst)
{
	int j;
	int dst_stride = w * 4;
	src += y * src_stride + x * 4;
	for (j = 0; j < h; j++) {
		memcpy(dst, src, dst_stride);
		src += src_stride;
		dst += dst_stride;
	}
}
