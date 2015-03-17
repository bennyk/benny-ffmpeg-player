/*
 * MainActivity.java
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

package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegDisplay;
import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegError;
import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegListener;
import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegPlayer;
import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegStreamInfo;
import com.bennykhoo.ffmpeg.myffmpeglibrary.FFmpegStreamInfo.CodecType;
import com.bennykhoo.ffmpeg.myffmpeglibrary.NotPlayingException;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class VideoActivity extends Activity implements OnClickListener,
		FFmpegListener, OnSeekBarChangeListener, OnItemSelectedListener {
	
	private static final String[] PROJECTION = new String[] {"title", BaseColumns._ID};
	private static final int PROJECTION_ID = 1;
    private static final String TAG = "VideoActivity";
    private static final int IPD_TICK = 10;

    private FFmpegPlayer mMpegPlayer;
	protected boolean mPlay = false;
	private View mControlsView;
	private View mLoadingView;
	private SeekBar mSeekBar;
	private View mVideoView;
	private Button mPlayPauseButton;
	private boolean mTracking = false;
	private View mStreamsView;
	private Spinner mLanguageSpinner;
	private int mLanguageSpinnerSelectedPosition = 0;
	private Spinner mSubtitleSpinner;
	private int mSubtitleSpinnerSelectedPosition = 0;
	private SimpleCursorAdapter mLanguageAdapter;
	private SimpleCursorAdapter mSubtitleAdapter;

	private int mAudioStreamNo = FFmpegPlayer.UNKNOWN_STREAM;
	private int mSubtitleStreamNo = FFmpegPlayer.NO_STREAM;
	private View mScaleButton;
	private long mCurrentTimeUs;
    private View mCoverView;

    private static final int KEY_UP = 19;
    private static final int KEY_DOWN = 20;
    private int _ipdDelta = 0;
    private int _ipdPX;
    private TextView mFpsLabel;
    
    private SensorManager _sensorManager;
    private LookAtSensorEventListener _lookAtSensorEventListener;
    private Sensor _accelerometer;
    private Sensor _magnetometer;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DITHER);

		super.onCreate(savedInstanceState);

		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		this.getWindow().setBackgroundDrawable(null);

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		this.setContentView(R.layout.video_surfaceview);

		mSeekBar = (SeekBar) this.findViewById(R.id.seek_bar);
		mSeekBar.setOnSeekBarChangeListener(this);

		mPlayPauseButton = (Button) this.findViewById(R.id.play_pause);
		mPlayPauseButton.setOnClickListener(this);
		
		mScaleButton = this.findViewById(R.id.scale_type);
		mScaleButton.setOnClickListener(this);
		
		mControlsView = this.findViewById(R.id.controls);
		mStreamsView = this.findViewById(R.id.streams);
		mLoadingView = this.findViewById(R.id.loading_view);
		mLanguageSpinner = (Spinner) this.findViewById(R.id.language_spinner);
		mSubtitleSpinner = (Spinner) this.findViewById(R.id.subtitle_spinner);

		mLanguageAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, null, PROJECTION,
				new int[] { android.R.id.text1 }, 0);
		mLanguageAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mLanguageSpinner.setAdapter(mLanguageAdapter);
		mLanguageSpinner.setOnItemSelectedListener(this);

		mSubtitleAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, null, PROJECTION,
				new int[] { android.R.id.text1 }, 0);
		mSubtitleAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mSubtitleSpinner.setAdapter(mSubtitleAdapter);
		mSubtitleSpinner.setOnItemSelectedListener(this);

        mMpegPlayer = new FFmpegPlayer(this);
		mMpegPlayer.setMpegListener(this);

        SurfaceView surfaceView = (SurfaceView) this.findViewById(R.id.video_view);

        mMpegPlayer.attachView((FFmpegDisplay) surfaceView);
		setDataSource();

        // container for our multi surface views
        mVideoView = surfaceView;

        mCoverView = this.findViewById(R.id.cover_view);
        mCoverView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControls();
            }
        });

        mFpsLabel = (TextView) this.findViewById(R.id.fpsLabel);

        _sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        _accelerometer = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        _magnetometer = _sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

	@Override
	protected void onPause() {
		super.onPause();

        _sensorManager.unregisterListener(_lookAtSensorEventListener);
    };

	@Override
	protected void onResume() {
		super.onResume();

        // setup aligning listener. Start panning camera when orientation is initialized properly.
        final InitSensorEventListener aligningSensorEventListener = new InitSensorEventListener(new InitSensorEventListener.FinishCallback() {
            @Override
            public void finish(InitSensorEventListener listener, float[] orientation) {
                _sensorManager.unregisterListener(listener);
                Log.i(TAG, "received stable orientation: " + orientation[0] + " " + orientation[1] + " " + orientation[2]);
                _lookAtSensorEventListener = new LookAtSensorEventListener(orientation, new LookAtSensorEventListener.LookAtCallback() {
                    @Override
                    public void lookAt(float azimuth, float pitch, float roll) {
                        mMpegPlayer.setLookatAngles(azimuth, pitch, roll);
                    }
                });

                _sensorManager.registerListener(_lookAtSensorEventListener, _accelerometer, SensorManager.SENSOR_DELAY_GAME);
                _sensorManager.registerListener(_lookAtSensorEventListener, _magnetometer, SensorManager.SENSOR_DELAY_GAME);

                // play click sound?
//                View v = findViewById(R.id.surfaceview);
//                v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            }
        });

        _sensorManager.registerListener(aligningSensorEventListener, _accelerometer, SensorManager.SENSOR_DELAY_GAME);
        _sensorManager.registerListener(aligningSensorEventListener, _magnetometer, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
		super.onDestroy();
		this.mMpegPlayer.setMpegListener(null);
		this.mMpegPlayer.stop();
		stop();
	}

	private void setDataSource() {
		HashMap<String, String> params = new HashMap<String, String>();
		
		// set font for ass
		File assFont = new File(Environment.getExternalStorageDirectory(),
				"DroidSansFallback.ttf");
		params.put("ass_default_font_path", assFont.getAbsolutePath());
		
		Intent intent = getIntent();
		Uri uri = intent.getData();
		String url;
		if (uri != null) {
			url = uri.toString();
		} else {
			url = intent
					.getStringExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL);
			if (url == null) {
				throw new IllegalArgumentException(String.format(
						"\"%s\" did not provided",
						AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL));
			}
			if (intent
					.hasExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_ENCRYPTION_KEY)) {
				params.put(
						"aeskey",
						intent.getStringExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_ENCRYPTION_KEY));
			}
		}

		this.mPlayPauseButton
		.setBackgroundResource(android.R.drawable.ic_media_play);
		this.mPlayPauseButton.setEnabled(true);
		mPlay = false;

		mMpegPlayer.setDataSource(url, params, FFmpegPlayer.UNKNOWN_STREAM, mAudioStreamNo,
				mSubtitleStreamNo);

	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.play_pause:
			resumePause();
			return;
		case R.id.scale_type:
			return;
		default:
			throw new RuntimeException();
		}
	}

	@Override
	public void onFFUpdateTime(long currentTimeUs, long videoDurationUs, boolean isFinished) {
		mCurrentTimeUs = currentTimeUs;
		if (!mTracking) {
			int currentTimeS = (int)(currentTimeUs / 1000 / 1000);
			int videoDurationS = (int)(videoDurationUs / 1000 / 1000);
			mSeekBar.setMax(videoDurationS);
			mSeekBar.setProgress(currentTimeS);
		}
		
		if (isFinished) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.dialog_end_of_video_title)
					.setMessage(R.string.dialog_end_of_video_message)
					.setCancelable(true).show();
		}
	}

    @Override
    public void onFFUpdateFps(double averageFps, double currentFps, int skippedFrames) {
        if (mFpsLabel.getVisibility() == View.VISIBLE) {
            Formatter formatter;
            if (skippedFrames > 0) {
                formatter = new Formatter().format("c %d a %.2f s %d", Math.round(currentFps), averageFps, skippedFrames);
            } else {
                formatter = new Formatter().format("c %d a %.2f", Math.round(currentFps), averageFps);
            }
            mFpsLabel.setText(formatter.toString());
        }
    }

    @Override
	public void onFFDataSourceLoaded(FFmpegError err, FFmpegStreamInfo[] streams) {
		if (err != null) {
			String format = getResources().getString(
					R.string.main_could_not_open_stream);
			String message = String.format(format, err.getMessage());

			Builder builder = new AlertDialog.Builder(VideoActivity.this);
			builder.setTitle(R.string.app_name)
					.setMessage(message)
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									VideoActivity.this.finish();
								}
							}).show();
			return;
		}
		mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
		mPlayPauseButton.setEnabled(true);
		this.mControlsView.setVisibility(View.VISIBLE);
		this.mStreamsView.setVisibility(View.VISIBLE);
		this.mLoadingView.setVisibility(View.GONE);
		MatrixCursor audio = new MatrixCursor(PROJECTION);
		MatrixCursor subtitles = new MatrixCursor(PROJECTION);
		subtitles.addRow(new Object[] {"None", FFmpegPlayer.NO_STREAM});
		for (FFmpegStreamInfo streamInfo : streams) {
			CodecType mediaType = streamInfo.getMediaType();
			Locale locale = streamInfo.getLanguage();
			String languageName = locale == null ? getString(
					R.string.unknown) : locale.getDisplayLanguage();
			if (FFmpegStreamInfo.CodecType.AUDIO.equals(mediaType)) {
				audio.addRow(new Object[] {languageName, streamInfo.getStreamNumber()});
			} else if (FFmpegStreamInfo.CodecType.SUBTITLE.equals(mediaType)) {
				subtitles.addRow(new Object[] {languageName, streamInfo.getStreamNumber()});
			}
		}
		mLanguageAdapter.swapCursor(audio);
		mSubtitleAdapter.swapCursor(subtitles);
	}

	private void displaySystemMenu(boolean visible) {
		if (Build.VERSION.SDK_INT >= 14) {
			displaySystemMenu14(visible);
		} else if (Build.VERSION.SDK_INT >= 11) {
			displaySystemMenu11(visible);
		}
	}

	@SuppressWarnings("deprecation")
	@TargetApi(11)
	private void displaySystemMenu11(boolean visible) {
		if (visible) {
			this.mVideoView.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
		} else {
			this.mVideoView.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
		}
	}

	@TargetApi(14)
	private void displaySystemMenu14(boolean visible) {
		if (visible) {
			this.mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		} else {
			this.mVideoView
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	public void resumePause() {
		this.mPlayPauseButton.setEnabled(false);
		if (mPlay) {
			mMpegPlayer.pause();
		} else {
			mMpegPlayer.resume();
			displaySystemMenu(true);

            hideControls();
		}
		mPlay = !mPlay;
	}

    public void hideControls() {
        Log.i(TAG, "hiding controls");

        TranslateAnimation translate1 = new TranslateAnimation(0, 0, 0, this.mControlsView.getHeight());
        translate1.setDuration(500);
        translate1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mControlsView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mControlsView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        this.mControlsView.startAnimation(translate1);


        TranslateAnimation translate2 = new TranslateAnimation(0, 0, 0, -this.mStreamsView.getHeight());
        translate2.setDuration(500);
        translate2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mStreamsView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mStreamsView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.mStreamsView.startAnimation(translate2);

//        this.mCoverView.setVisibility(View.VISIBLE);
    }

    public void exposeControls() {
        Log.i(TAG, "exposing controls");

        TranslateAnimation translate1 = new TranslateAnimation(0, 0, this.mControlsView.getHeight(), 0);
        translate1.setDuration(500);
        mControlsView.setVisibility(View.VISIBLE);
        this.mControlsView.startAnimation(translate1);

        TranslateAnimation translate2 = new TranslateAnimation(0, 0, -this.mStreamsView.getHeight(), 0);
        translate2.setDuration(500);
        this.mStreamsView.setVisibility(View.VISIBLE);
        this.mStreamsView.startAnimation(translate2);

//        this.mCoverView.setVisibility(View.GONE);
    }

    public void toggleControls() {
        if (this.mControlsView.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            exposeControls();
        }
    }

	@Override
	public void onFFResume(NotPlayingException result) {
		this.mPlayPauseButton
				.setBackgroundResource(android.R.drawable.ic_media_pause);
		this.mPlayPauseButton.setEnabled(true);

		displaySystemMenu(false);
		mPlay = true;
	}

	@Override
	public void onFFPause(NotPlayingException err) {
		this.mPlayPauseButton
				.setBackgroundResource(android.R.drawable.ic_media_play);
		this.mPlayPauseButton.setEnabled(true);
		mPlay = false;
	}

	private void stop() {
		this.mControlsView.setVisibility(View.GONE);
		this.mStreamsView.setVisibility(View.GONE);
		this.mLoadingView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onFFStop() {
	}

	@Override
	public void onFFSeeked(NotPlayingException result) {
//		if (result != null)
//			throw new RuntimeException(result);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			long timeUs = progress * 1000 * 1000;
			mMpegPlayer.seek(timeUs);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mTracking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mTracking = false;
	}
	
	private void setDataSourceAndResumeState() {
		setDataSource();
		mMpegPlayer.seek(mCurrentTimeUs);
		mMpegPlayer.resume();
	}

	@Override
	public void onItemSelected(AdapterView<?> parentView,
			View selectedItemView, int position, long id) {
		Cursor c = (Cursor) parentView
				.getItemAtPosition(position);
		if (parentView == mLanguageSpinner) {
			if (mLanguageSpinnerSelectedPosition != position) {
				mLanguageSpinnerSelectedPosition = position;
				mAudioStreamNo = c.getInt(PROJECTION_ID);
				setDataSourceAndResumeState();
			}
		} else if (parentView == mSubtitleSpinner) {
			if (mSubtitleSpinnerSelectedPosition != position) {
				mSubtitleSpinnerSelectedPosition = position;
				mSubtitleStreamNo = c.getInt(PROJECTION_ID);
				setDataSourceAndResumeState();
			}
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parentView) {
		// if (parentView == languageSpinner) {
		// audioStream = null;
		// } else if (parentView == subtitleSpinner) {
		// subtitleStream = null;
		// } else {
		// throw new RuntimeException();
		// }
		// play();
	}

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            // restore from setting file when views are stable and sized properly.
            parseSettings();
        }
    }

    public void parseSettings() {

        String data = null;
        if (getIPDConfigFile().exists()) {
            try {
                data = getStringFromFile(getIPDConfigFile());
                JSONObject json = new JSONObject(data);
                Double ipdMM = json.getDouble("ipdMM");

                DisplayMetrics metrics = getResources().getDisplayMetrics();
                _ipdPX = (int) Math.round(ipdMM * metrics.xdpi / 25.4f); // * 2 for both side

                Log.d(TAG, "parsed IPD values: " + ipdMM + " mm " + _ipdPX + "px");

                SurfaceView surfaceView = (SurfaceView) this.findViewById(R.id.video_view);
                Log.d(TAG, "surface view width = " + surfaceView.getWidth());

                mMpegPlayer.setIPDPx(_ipdPX);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File getIPDConfigFile() {
        File path = Environment.getExternalStorageDirectory();
        path.mkdirs();
        File file = new File(path, "ipd.conf");
        return file;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (File file) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        boolean flag = false;

        Log.d(TAG, "keyDown " + keyCode);
        switch (keyCode) {
            case KEY_DOWN:
                flag = true;
                _ipdDelta += -IPD_TICK;
                break;
            case KEY_UP:
                flag = true;
                _ipdDelta += IPD_TICK;
                break;

            default:
                Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
                v.vibrate(50);

        }

        if (flag) {
            mMpegPlayer.setIPDPx(_ipdPX + _ipdDelta);
        }

        return super.onKeyDown(keyCode, event);
    }

    static final float ALPHA = 0.2f; // if ALPHA = 1 OR 0, no filter applies.

    static protected float[] LowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    static class InitSensorEventListener implements SensorEventListener {
        public interface FinishCallback {
            public void finish(InitSensorEventListener listener, float[] orientation);
        }

        private FinishCallback _finishCallback;

        private float[] _lastAccelSet;
        private float[] _lastMagnetoSet;
        private float[] _r = new float[9];

        private float[] _lastOrientationSet;
        private float[] _summedOrientationSet;
        private int _readingCount;
        private long _startTime;
        private final float errorThreshold = (float) (15.0f/180.0f * Math.PI);

        public InitSensorEventListener(FinishCallback _callback) {
            this._finishCallback = _callback;
            start();
        }

        void start() {
            _lastAccelSet = null;
            _lastMagnetoSet = null;
            _summedOrientationSet = new float[3];
            _lastOrientationSet = null;
            _readingCount = 0;
            _startTime = System.currentTimeMillis();
        }

        void finish() {
            float[] finalOrientation = new float[3];
            for (int i = 0; i < 3; i++) {
                finalOrientation[i] = _summedOrientationSet[i] / _readingCount;
            }
            _finishCallback.finish(this, finalOrientation);
        }

        void readOrientation(float []orientation) {
            for (int i = 0; i < 3; i++) {
                _summedOrientationSet[i] += orientation[i];
            }
            _readingCount++;
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                _lastAccelSet = LowPass(event.values.clone(), _lastAccelSet);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                _lastMagnetoSet = LowPass(event.values.clone(), _lastMagnetoSet);
            }

            if (_lastAccelSet != null && _lastMagnetoSet != null) {
                SensorManager.getRotationMatrix(_r, null, _lastAccelSet, _lastMagnetoSet);
                float[] orientation = new float [3];
                SensorManager.getOrientation(_r, orientation);

                // if erratic reading restart timer
                boolean okay = true;
                if (_lastOrientationSet != null) {
                    float[] err = new float[3];
                    for (int i = 0; i < 3; i++) {
                        err[i] = orientation[i] - _lastOrientationSet[i];
                        if (err[i] > errorThreshold) {
                            Log.w(TAG, "erratic reading at indice " + i + " with error " + err[i] + " more than preset threshold. Restarting timer");
                            start();
                            okay = false;
                            break;
                        }
                    }
                }

                if (okay) {
                    readOrientation(orientation);
                    long elapsedTime = System.currentTimeMillis() - _startTime;
                    if (elapsedTime > 3000) {
                        // set offset
                        finish();
                    }
                }
                _lastOrientationSet = orientation;
            }
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    static class LookAtSensorEventListener implements SensorEventListener {

        public interface LookAtCallback {
            public void lookAt(float azimuth, float pitch, float roll);
        }

        private LookAtCallback _lookAtCallback;
        private float[] _lastAccelSet;
        private float[] _lastMagnetoSet;
        private float[] _r = new float[9];
        private final float[] _offsets;

        LookAtSensorEventListener(float[] offsets, LookAtCallback _lookAtCallback) {
            this._lookAtCallback = _lookAtCallback;
            this._offsets = offsets;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                _lastAccelSet = LowPass(event.values.clone(), _lastAccelSet);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                _lastMagnetoSet = LowPass(event.values.clone(), _lastMagnetoSet);
            }

            if (_lastAccelSet != null && _lastMagnetoSet != null) {
                SensorManager.getRotationMatrix(_r, null, _lastAccelSet, _lastMagnetoSet);
                float[] orientation = new float[3];
                SensorManager.getOrientation(_r, orientation);

                _lookAtCallback.lookAt(_offsets[0] - orientation[0],
                        orientation[1] - _offsets[1],
                        _offsets[2] - orientation[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }




}
