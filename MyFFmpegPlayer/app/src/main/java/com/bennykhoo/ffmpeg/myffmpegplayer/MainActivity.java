package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import eu.erikw.PullToRefreshListView;

public class MainActivity extends ActionBarActivity implements OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String CONNECT_DIALOG_TAG = "ConnectDialogTag";
    public static final String PREFS_NAME = "MyPrefsDB";

    private static final int PICKFILE_RESULT_CODE = 123;

    private PullToRefreshListView mListView;
	private CursorAdapter mAdapter;

    private VideoListSQLiteHelper dbHelper;
    private String[] allColumns = { VideoListSQLiteHelper.COLUMN_ID,
            VideoListSQLiteHelper.COLUMN_TITLE,
            VideoListSQLiteHelper.COLUMN_URL,
            VideoListSQLiteHelper.COLUMN_ENC_KEY };

    // Exhaustive listing of video file extension according to http://en.wikipedia.org/wiki/Video_file_format
    private static final String[] mediaFileExtensionsArray = new String[] { ".webm", ".mkv", ".flv",
            // TODO ogg file doesn't play now hmmm...
            //".ogv", ".ogg", ".ogv",
            ".drc", ".mng", ".avi", ".mov", ".qt", ".wmv", ".yuv", ".rm", ".rmvb", ".asf", ".mp4", ".m4p", ".m4v", ".mpg", ".mp2", ".mpeg", ".mpe", ".mpv", ".m2v", ".m4v", ".svi", ".3gp", ".3g2", ".mxf", ".roq", ".nsv"
    };
    private static Set<String> mediaFileExtensionsHash = new HashSet<String>(Arrays.asList(mediaFileExtensionsArray));

    private static String getLastPathFragment(String fname) {
        String result = fname;
        int lastIndex = fname.lastIndexOf("/");
        if (lastIndex >= 0) {
            result = fname.substring(lastIndex + 1);
        }
        return result;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

        dbHelper = new VideoListSQLiteHelper(this);
        mAdapter = new MainAdapter(this);

		mListView = (PullToRefreshListView) findViewById(android.R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
        mListView.setTextRefreshing("Scanning for media files...");
//        mListView.setRefreshing();

        mListView.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.w(TAG, "refreshing");
                AsyncTask<File, Void, Void> scanTask = new AsyncTask<File, Void, Void>() {

                    void startScanning(File startDir) {
                        LinkedList<File> q = new LinkedList<>();

                        q.add(startDir);

                        while (q.size() > 0) {
                            File current = q.removeFirst();
                            File[] list = current.listFiles();

                            for (File f : list) {
                                if (f.isDirectory()) {
//                                    Log.d("", "Dir: " + f.getAbsoluteFile());
                                    q.add(f);
                                } else {
                                    String filename = f.toString();

//                                    Log.d("", "file: " + filename);
                                    int lastIndex = filename.lastIndexOf(".");
                                    if (lastIndex >= 0) {
                                        String ext = filename.substring(lastIndex);
                                        if (mediaFileExtensionsHash.contains(ext)) {
                                            Log.d("", "found a media file: " + f.getAbsoluteFile());

                                            // add an entry to our cache database.
                                            addURL(getLastPathFragment(filename), filename, null);
                                        }
                                    }
                                }
                            }
                        }

                    }

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        clearAll();
                    }

                    @Override
                    protected Void doInBackground(File... files) {
                        for (int i = 0; i < files.length; i++) {
                            startScanning(files[i]);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        refresh();
                        mListView.onRefreshComplete();
                    }
                };

                scanTask.execute(Environment.getExternalStorageDirectory());
            }
        });

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

    void clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(VideoListSQLiteHelper.TABLE_VIDEOS, "", null);
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
