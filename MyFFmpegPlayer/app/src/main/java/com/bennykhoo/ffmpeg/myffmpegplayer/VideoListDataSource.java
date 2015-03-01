package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bennykhoo on 2/26/15.
 */
public class VideoListDataSource {

    private SQLiteDatabase database;
    private VideoListSQLiteHelper dbHelper;
    private String[] allColumns = { VideoListSQLiteHelper.COLUMN_ID,
            VideoListSQLiteHelper.COLUMN_TITLE, VideoListSQLiteHelper.COLUMN_URL, VideoListSQLiteHelper.COLUMN_ENC_KEY };

    public VideoListDataSource(Context context) {
        dbHelper = new VideoListSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public VideoURL createURL(String title, String url, String encKey) {
        ContentValues values = new ContentValues();
        values.put(VideoListSQLiteHelper.COLUMN_TITLE, title);
        values.put(VideoListSQLiteHelper.COLUMN_URL, url);
        values.put(VideoListSQLiteHelper.COLUMN_ENC_KEY, encKey);

        long insertId = database.insert(VideoListSQLiteHelper.TABLE_VIDEOS, null,
                values);
        Cursor cursor = database.query(VideoListSQLiteHelper.TABLE_VIDEOS,
                allColumns, VideoListSQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        VideoURL videoURL = cursorToVideoURL(cursor);
        cursor.close();
        return videoURL;
    }

    public void deleteComment(VideoURL videoURL) {
        long id = videoURL.getId();
        System.out.println("Comment deleted with id: " + id);
        database.delete(VideoListSQLiteHelper.TABLE_VIDEOS, VideoListSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<VideoURL> getAll() {
        List<VideoURL> comments = new ArrayList<VideoURL>();

        Cursor cursor = database.query(VideoListSQLiteHelper.TABLE_VIDEOS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            VideoURL comment = cursorToVideoURL(cursor);
            comments.add(comment);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return comments;
    }

    private VideoURL cursorToVideoURL(Cursor cursor) {
        VideoURL comment = new VideoURL();
        comment.setId(cursor.getLong(0));
        comment.setTitle(cursor.getString(1));
        comment.setUrl(cursor.getString(2));
        comment.setEncKey(cursor.getString(3));

        return comment;
    }

}
