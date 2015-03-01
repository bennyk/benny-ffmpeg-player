package com.bennykhoo.ffmpeg.myffmpegplayer;

/**
 * Created by bennykhoo on 2/26/15.
 */
public class VideoURL {
    private long id;
    private String title;
    private String url;
    private String encKey;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String encKey) {
        this.encKey = encKey;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        if (title == null) {
            return "null";
        }
        return title;
    }
}
