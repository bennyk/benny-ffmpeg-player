package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.bennykhoo.ffmpeg.myffmpegplayer.adapter.MainAdapter;

import java.io.File;

public class MainActivity extends ActionBarActivity implements OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String CONNECT_DIALOG_TAG = "ConnectDialogTag";
    public static final String PREFS_NAME = "MyPrefsDB";

    private static final int PICKFILE_RESULT_CODE = 123;

    private ListView mListView;
	private CursorAdapter mAdapter;

    private VideoListSQLiteHelper dbHelper;
    private String[] allColumns = { VideoListSQLiteHelper.COLUMN_ID,
            VideoListSQLiteHelper.COLUMN_TITLE,
            VideoListSQLiteHelper.COLUMN_URL,
            VideoListSQLiteHelper.COLUMN_ENC_KEY };

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

        dbHelper = new VideoListSQLiteHelper(this);
        mAdapter = new MainAdapter(this);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);

        refresh();
    }

	private static String getSDCardFile(String file) {
		File videoFile = new File(Environment.getExternalStorageDirectory(),
				file);
		String a = "file://" + videoFile.getAbsolutePath();
        Log.v("MainActivity", "sdcard file " + a);
        return a;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		String url = cursor.getString(MainAdapter.PROJECTION_URL);
		Intent intent = new Intent(AppConstants.VIDEO_PLAY_ACTION);
		intent.putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL, url);
		String encryptionKey = cursor.getString(MainAdapter.PROJECTION_ENCRYPTION_KEY);
		if (encryptionKey != null) {
			intent.putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_ENCRYPTION_KEY, encryptionKey);
		}
		startActivity(intent);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                ConnectDialogFragment dlg = new ConnectDialogFragment();
                dlg.show(getFragmentManager(), CONNECT_DIALOG_TAG);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_RESULT_CODE) {
            if (resultCode != 0) {
                Uri dat = data.getData();
                addURL(dat.getLastPathSegment(), Uri.decode(dat.toString()), null);
                refresh();
            } else {
                Log.w(TAG, "nothing has been selected");
            }
        }
        else {
            Log.e(TAG, "not known request code: " + requestCode);
        }

    }

    void addURL(String title, String url, String encKey) {
        ContentValues values = new ContentValues();
        values.put(VideoListSQLiteHelper.COLUMN_TITLE, title);
        values.put(VideoListSQLiteHelper.COLUMN_URL, url);
        values.put(VideoListSQLiteHelper.COLUMN_ENC_KEY, encKey);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long insertId = db.insert(VideoListSQLiteHelper.TABLE_VIDEOS, null,
                values);
    }

    void refresh() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(VideoListSQLiteHelper.TABLE_VIDEOS,
                allColumns, null, null, null, null, null);
        mAdapter.swapCursor(cursor);
    }

    void startPickFileActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    void playUri(Uri uri) {
        Intent intent = new Intent(AppConstants.VIDEO_PLAY_ACTION);
        intent.putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL, Uri.decode(uri.toString()));
        startActivity(intent);
    }
}
