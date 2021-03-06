/*
 * FFmpegSurfaceView.java
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

package com.bennykhoo.ffmpeg.myffmpeglibrary;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FFmpegSurfaceView extends SurfaceView implements FFmpegDisplay,
		SurfaceHolder.Callback {
    private String TAG = "FFmpegSurfaceView";

	public static enum ScaleType {
		CENTER_CROP, CENTER_INSIDE, FIT_XY
	}

	private FFmpegPlayer mMpegPlayer = null;
	private boolean mCreated = false;
    private AttachmentSide attachmentSide;

	public FFmpegSurfaceView(Context context) {
		this(context, null, 0);
	}

	public FFmpegSurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FFmpegSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		SurfaceHolder holder = getHolder();
		holder.setFormat(PixelFormat.RGBA_8888);
		holder.addCallback(this);
	}

	@Override
	public void setMpegPlayer(FFmpegPlayer fFmpegPlayer, AttachmentSide side) {
		if (mMpegPlayer != null)
			throw new RuntimeException(
					"setMpegPlayer could not be called twice");

		this.mMpegPlayer = fFmpegPlayer;
        this.attachmentSide = side;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mCreated  == true) {
			surfaceDestroyed(holder);
		}

		Surface surface = holder.getSurface();
        if (attachmentSide == AttachmentSide.LEFT) {
            Log.i(TAG, "attaching left surface");
            mMpegPlayer.attachSurface1(surface);
        }
        else if (attachmentSide == AttachmentSide.RIGHT) {
            Log.i(TAG, "attaching right surface");
            mMpegPlayer.attachSurface2(surface);
        }
        else {
            throw new RuntimeException(
                    "invalid attachment side " + attachmentSide);
        }
		mCreated = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		this.mMpegPlayer.renderFrameStop();
		mCreated = false;
	}

}
