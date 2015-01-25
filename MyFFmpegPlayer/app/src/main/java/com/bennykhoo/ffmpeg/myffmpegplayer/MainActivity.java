package com.bennykhoo.ffmpeg.myffmpegplayer;

import java.io.File;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
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

public class MainActivity extends ActionBarActivity implements OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String CONNECT_DIALOG_TAG = "ConnectDialogTag";
    public static final String PREFS_NAME = "MyPrefsDB";

    private ListView mListView;
	private CursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		MatrixCursor cursor = new MatrixCursor(MainAdapter.PROJECTION);
        /*
		cursor.addRow(new Object[] {
				1,
				"Kings Of Leon-Charmer unencrypted",
				getSDCardFile("airbender/videos/Videoguides-Riga_SIL_engrus_1500.mp4"),
				null });
		cursor.addRow(new Object[] {
				2,
				"TheThreeStooges",
				"http://192.168.0.200:81/TheThreeStooges_ENGRUS_engjapchi.mp4",
				null });
		cursor.addRow(new Object[] {
				3,
				"Apple sample",
				"http://devimages.apple.com.edgekey.net/resources/http-streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
				null });
		cursor.addRow(new Object[] {
				4,
				"Apple advenced sample",
				"https://devimages.apple.com.edgekey.net/resources/http-streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
				null });
		cursor.addRow(new Object[] {
				5,
				"Encrypted file",
				getSDCardFile("encrypted.mp4"),
				"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" });
		cursor.addRow(new Object[] {
				6,
				"JWillIAm-CheckItOut_ENG.mp4",
				getSDCardFile("airbender/videos/WillIAm-CheckItOut_ENG.mp4"),
				null });
		cursor.addRow(new Object[] {
				7,
				"HungerGamesTrailer1200.mp4",
				getSDCardFile("airbender/videos/HungerGamesTrailer1200.mp4"),
				null });
		cursor.addRow(new Object[] {
				8,
				"HungerGamesTrailer800.mp4",
				getSDCardFile("airbender/videos/HungerGamesTrailer800.mp4"),
				null });
		cursor.addRow(new Object[] {
				9,
				"TheThreeStooges_ENGRUS_engjapchi.mp4",
				getSDCardFile("airbender/videos/TheThreeStooges_ENGRUS_engjapchi.mp4"),
				null });
		cursor.addRow(new Object[] {
				10,
				"Jasmine Sullivan unencrypted",
				getSDCardFile("airbender/videos/JasmineSullivan-DreamBig_ENG-ENCODESTREAM.mp4"),
				null });
		cursor.addRow(new Object[] {
				11,
				"Jennifer Hudson unencrypted",
				getSDCardFile("airbender/videos/JenniferHudson-IfThisIsntLove_ENG-ENCODESTREAM.mp4"),
				null });
		cursor.addRow(new Object[] {
				12,
				"Kings Of Leon-Charmer unencrypted",
				getSDCardFile("airbender/videos/KingsOfLeon-Charmer_ENG-ENCODESTREAM.mp4"),
				null });
		cursor.addRow(new Object[] {
				13,
				"Kings Of Leon-Charmer unencrypted",
				getSDCardFile("airbender/videos/Lenka-TheShow_ENG-ENCODESTREAM.mp4"),
				null });
		cursor.addRow(new Object[] {
				14,
				"ThreeMenInABoatToSayNothingOfTheDog_RUS_eng_1500.mp4.enc",
				getSDCardFile("airbender/videos/ThreeMenInABoatToSayNothingOfTheDog_RUS_eng_1500.mp4.enc"),
				"fNFyiU34+Pw4iU6QqazxUZ/+pUMWXQTq" });
				*/

        cursor.addRow(new Object[] {
                1,
                "Test RTMP stream",
                "rtmp://10.0.1.7:1935/live/test",
                null });
        cursor.addRow(new Object[] {
                2,
                "Asus Demo",
                "/sdcard/Movies/m_ASUS_Display_Demo.mp4",
                null });
        cursor.addRow(new Object[] {
                3,
                "Dawn of the Planet of the Apes",
                "/sdcard/daa-sample.of.the.release.of.the.daawn.of.the.planet.of.the.rapes-720p.mkv",
                null });

        cursor.addRow(new Object[] {
                4,
                "Fury 2014",
                "/sdcard/fury.2014.720p.bluray.x264-sparks.sample.mkv",
                null });

        cursor.addRow(new Object[] {
                4,
                "Roller Coaster",
                "/sdcard/roller-coaster.mp4",
                null });

        mAdapter = new MainAdapter(this);
		mAdapter.swapCursor(cursor);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
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
}
