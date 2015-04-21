package com.bennykhoo.ffmpeg.myffmpegplayer;


import android.app.Activity;
import android.content.SharedPreferences;

/**
 * Created by bennykhoo on 3/21/15.
 */
public class SettingsBook {
    public static final String SETTINGS_SHADER_MODE = "Settings.shaderMode";
    public static final String SETTINGS_SCREEN_MODE = "Settings.screenMode";

    public static final int END_FLAG = 0xfeff;
    public static final int SHADER_MODE_FLAG = 1;
    public static final int SCREEN_MODE_FLAG = 2;

    public static int [] getSettingsFlags(Activity activity) {
        int [] result = {
                SHADER_MODE_FLAG, getSavedOption(activity, SETTINGS_SHADER_MODE),
                SCREEN_MODE_FLAG, getSavedOption(activity, SETTINGS_SCREEN_MODE),
                END_FLAG};

        return result;
    }

    static int getSavedOption(Activity activity, String name) {
        SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settings.getInt(name + ".value", 0);
    }




}
