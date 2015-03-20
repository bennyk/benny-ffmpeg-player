package com.bennykhoo.ffmpeg.myffmpegplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by bennykhoo on 3/20/15.
 */
public class SettingsActivity extends ActionBarActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ExpandableListView listView = (ExpandableListView) findViewById(R.id.listview);
        listView.setDividerHeight(2);
        listView.setClickable(true);

        ArrayList<SettingItem> settingsData = prepareSettings();
        SettingsAdapter adapter = new SettingsAdapter(settingsData);
        adapter.setInflater(
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                this);
        listView.setAdapter(adapter);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Integer width = metrics.widthPixels;

        //this code for adjusting the group indicator into right side of the view
        // see http://stackoverflow.com/questions/5132699/android-how-to-change-the-position-of-expandablelistview-indicator
        listView.setIndicatorBoundsRelative(width - 39, width - 15);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.actionbar_space_between_icon_and_title);
    }

    ArrayList<SettingItem> prepareSettings() {
        ArrayList<SettingItem> data = new ArrayList<>();

        SettingItem myItem = new SettingItem("Shader mode", "Settings.shaderMode");
        myItem.add(new EnumItem("Auto", 0));
        myItem.add(new EnumItem("Normal", 1));
        myItem.add(new EnumItem("Anaglyphic", 2));
        myItem.add(new EnumItem("YUV", 3));
        myItem.add(new EnumItem("Test", 4));
        data.add(myItem);


        myItem = new SettingItem("Screen mode", "Settings.screenMode");
        myItem.add(new EnumItem("Stereoscopic", 0));
        myItem.add(new EnumItem("Full screen", 1));
        data.add(myItem);

        return data;
    }


    class EnumItem {
        public String title;
        Integer value;

        public View cachedView;

        EnumItem(String title, Integer value) {
            this.title = title;
            this.value = value;
        }
    }

    class SettingItem {
        String title;
        ArrayList<EnumItem> enumList = new ArrayList<>();
        Integer currentIndex;
        String name;
        public View cachedView;

        SettingItem(String title, String name) {
            this.title = title;
            this.name = name;

            restoreSavedIndex();
        }

        void add(EnumItem item) {
            enumList.add(item);
        }

        EnumItem get(Integer index) {
            return enumList.get(index);
        }

        ArrayList<EnumItem> getEnumList() {
            return enumList;
        }

        int size() {
            return enumList.size();
        }

        void restoreSavedIndex () {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            currentIndex = settings.getInt(name, 0);
        }

        void saveIndex() {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(name, currentIndex);

            // Commit the edits!
            editor.commit();
        }

    }

    class SettingsAdapter extends BaseExpandableListAdapter {

        private final ArrayList<SettingItem> _data;
        LayoutInflater _inflater;
        Activity activity;

        private boolean _protectFromCheckedChange;

        public SettingsAdapter(ArrayList<SettingItem> data) {
            _data = data;
        }

        public void setInflater(LayoutInflater inflater, Activity act) {
            this._inflater = inflater;
            activity = act;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            RadioButton text = null;
            final SettingItem childGroup = _data.get(groupPosition);

            EnumItem currentItem = childGroup.get(childPosition);

            if (currentItem.cachedView == null) {
                Log.i(TAG, "inflate pos " + groupPosition + " " + childPosition);
                View currentView = _inflater.inflate(R.layout.list_setting_item, null);
                currentView.setTag(currentItem);
                currentItem.cachedView = currentView;

                text = (RadioButton) currentView.findViewById(R.id.textView1);
                text.setText(currentItem.title);

                if (childPosition == childGroup.currentIndex) {
                    text.setChecked(true);
                }

                text.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (_protectFromCheckedChange) {
                            return;
                        }

                        EnumItem selectedItem = findEnumItemFromView(buttonView);
                        if (selectedItem == null) {
                            throw new IllegalArgumentException("Can't find enumerated item for the current selection");
                        }

                        childGroup.currentIndex = childPosition;
                        childGroup.saveIndex();

                        TextView valueView = (TextView) childGroup.cachedView.findViewById(R.id.groupValue);
                        valueView.setText(selectedItem.title);

                        _protectFromCheckedChange = true;
                        for (EnumItem enumItem : childGroup.getEnumList()) {
                            RadioButton rb = (RadioButton) enumItem.cachedView.findViewById(R.id.textView1);
                            if (rb != buttonView) {
                                if (rb.isChecked())
                                    rb.setChecked(false);
                            }
                        }
                        _protectFromCheckedChange = false;
                    }
                });

            }

            return currentItem.cachedView;
        }

        EnumItem findEnumItemFromView(View startingView) {
            View currentView = startingView;
            EnumItem result = null;

            do {
                Object tag = currentView.getTag();
                if (tag instanceof EnumItem) {
                    result = (EnumItem) tag;
                    break;
                }
                currentView = (View) currentView.getParent();
            } while (currentView.getParent() != null);

            return result;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return _data.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public int getGroupCount() {
            return _data.size();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
//            Log.i(TAG, "getGroupView " + groupPosition + " " + isExpanded + " " + convertView);

            final SettingItem currentItem = _data.get(groupPosition);
            View currentView = currentItem.cachedView;
            if (currentView == null) {
                currentView = _inflater.inflate(R.layout.list_setting_group, null);
                currentView.setTag(currentItem);
                currentItem.cachedView = currentView;

                TextView titleView = (TextView) currentView.findViewById(R.id.groupTitle);
                titleView.setText(currentItem.title);

                TextView valueView = (TextView) currentView.findViewById(R.id.groupValue);

                EnumItem selectedEnum = currentItem.get(currentItem.currentIndex);
                valueView.setText(selectedEnum.title);
            }
            return currentView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

    }

}
