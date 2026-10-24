package com.youyouboydragon.soundblocker;

import android.graphics.drawable.Drawable;

public class AudioSessionInfo {
    public final String packageName;
    public final String appName;
    public final String title;
    public final String artist;
    public final boolean isPlaying;
    public final boolean isBlocked;
    public final Drawable appIcon;

    public AudioSessionInfo(String packageName, String appName, String title, String artist, boolean isPlaying, boolean isBlocked, Drawable appIcon) {
        this.packageName = packageName;
        this.appName = appName;
        this.title = title;
        this.artist = artist;
        this.isPlaying = isPlaying;
        this.isBlocked = isBlocked;
        this.appIcon = appIcon;
    }
}
