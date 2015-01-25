package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


/**
 * Created by bennykhoo on 1/25/15.
 */
public class ConnectDialogFragment extends DialogFragment {

    private static final String TAG = "ConnectDialogFragment";
    private static final String PREFS_CONNECT_URL = "ConnectDialog.url";
    private TextView urlView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View layout = inflater.inflate(R.layout.dialog_connect, null);

        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String url = settings.getString(PREFS_CONNECT_URL, null);
        if (url == null) {
            url = "rtmp://0.0.0.0:1935/live/test";
        }
        urlView = (TextView) layout.findViewById(R.id.url);
        urlView.setText(url);

        builder.setTitle("Connect Stream");
        builder.setView(layout)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(AppConstants.VIDEO_PLAY_ACTION);
                        intent.putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL, urlView.getText().toString());
                        getActivity().startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "dismissing dialog");
        super.onDismiss(dialog);

        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        CharSequence url = urlView.getText();
        if (url != null) {
            editor.putString(PREFS_CONNECT_URL, url.toString());
        }

        // Commit the edits!
        editor.commit();

    }
}
