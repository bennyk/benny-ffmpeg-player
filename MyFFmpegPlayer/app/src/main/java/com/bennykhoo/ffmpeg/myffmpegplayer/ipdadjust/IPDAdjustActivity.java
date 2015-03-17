package com.bennykhoo.ffmpeg.myffmpegplayer.ipdadjust;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.bennykhoo.ffmpeg.myffmpegplayer.R;
import com.bennykhoo.ffmpeg.myffmpegplayer.ipdadjust.util.SystemUiHider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class IPDAdjustActivity extends Activity {
    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private static final String TAG = "IPDAdjustActivity";
    private static final int KEY_UP = 19;
    private static final int KEY_DOWN = 20;
    private static final int KEY_LEFT = 21;
    private static final int KEY_RIGHT = 22;
    private static final int VOLUME_UP = 24;
    private static final int VOLUME_DOWN = 25;
    private static final int KEY_BACK = 4;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private GestureDetectorCompat mDetector;
    private GroupedIPDView _groupedIPDView;

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

    class GroupedIPDView {
        private final IPDView _leftView;
        private final IPDView _rightView;

        public GroupedIPDView(IPDView leftView, IPDView rightView) {
            _leftView = leftView;
            _rightView = rightView;
        }

        public void adjustApart() {
            _leftView.adjustLeft();
            _rightView.adjustRight();

            saveSettings();
        }

        public void adjustCloser() {
            _leftView.adjustRight();
            _rightView.adjustLeft();

            saveSettings();
        }

        public void setOffsetFromCenter(int offset) {
            _rightView.setOffset(offset);
            _leftView.setOffset(_leftView.getWidth() - offset);
        }

        private File getIPDConfigFile() {
//            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File path = Environment.getExternalStorageDirectory();
            path.mkdirs();
            File file = new File(path, "ipd.conf");

//            File file = File.createTempFile("ipd", ".conf");
            return file;
        }

        private void saveSettings() {

            // convert IPD raw px to mm
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            double ipdMM = _rightView.getOffset() / metrics.xdpi * 25.4f * 2; // * 2 for both side

            _rightView.getOffset();
            try {
                JSONObject json = new JSONObject();
                json.put("ipdMM", ipdMM);
                Log.d(TAG, "saving IPD " + ipdMM + " mm " + _rightView.getOffset() + "px");

                File file = getIPDConfigFile();
                Log.d(TAG, "saving to " + file);

                OutputStream os = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(os);
                osw.write(json.toString());
                osw.close();

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
                    int ipdPX = (int) Math.round(ipdMM * metrics.xdpi / (25.4f * 2)); // * 2 for both side

                    Log.d(TAG, "parsed IPD values: " + ipdMM + " mm " + ipdPX + "px");

                    setOffsetFromCenter(ipdPX);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "MyGestureListener";
        private final GroupedIPDView _ipdView;

        MyGestureListener(GroupedIPDView ipdView) {
            _ipdView = ipdView;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(TAG, "onFling: " + event1.toString() + event2.toString());

            float dy = event2.getY() - event1.getY();
            if (Math.abs(dy) > IPDAdjustActivity.convertDpToPixel(50, getBaseContext())) {
                Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
                v.vibrate(50);
            } else {
                if (event2.getX() > event1.getX()) {
                    Log.d(TAG, "fling to right");
                    _ipdView.adjustCloser();
                } else {
                    Log.d(TAG, "fling to left");
                    _ipdView.adjustApart();
                }

            }
            return true;
        }
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();

        // Hide the status bar.

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_ipdadjust);

        final IPDView leftView = (IPDView) findViewById(R.id.left_view);
        final IPDView rightView = (IPDView) findViewById(R.id.right_view);
        _groupedIPDView = new GroupedIPDView(leftView, rightView);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener(_groupedIPDView));

        final View contentView = findViewById(R.id.fullscreen_content);
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        boolean flag = true;

//        Log.d(TAG, "keyDown " + keyCode);

        if (keyCode == KEY_BACK)
            return super.onKeyDown(keyCode, event);

        switch (keyCode) {
            case VOLUME_UP:
            case KEY_LEFT:
            case KEY_DOWN:
                _groupedIPDView.adjustApart();
                break;

            case VOLUME_DOWN:
            case KEY_RIGHT:
            case KEY_UP:
                _groupedIPDView.adjustCloser();
                break;

            default:
                flag = false;
                Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
                v.vibrate(50);

        }
        return flag;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // restore from setting file when views are stable and sized properly.
        _groupedIPDView.parseSettings();
    }
}
