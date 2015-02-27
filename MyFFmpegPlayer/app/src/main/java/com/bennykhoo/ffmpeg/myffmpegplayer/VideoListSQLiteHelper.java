package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by bennykhoo on 2/26/15.
 */
public class VideoListSQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = "VideoListSQLiteHelper";

    public static final String TABLE_VIDEOS = "videos";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_ENC_KEY = "enc_key";


    private static final String DATABASE_NAME = "videos.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_VIDEOS + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TITLE + " text, "
            + COLUMN_URL + " text not null, "
            + COLUMN_ENC_KEY + " text);";

    public VideoListSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "creating " + DATABASE_CREATE);
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(VideoListSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEOS);
        onCreate(db);
    }
}
