package com.youyouboydragon.soundblocker;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String packageName;
    public final String appName;
    public final Drawable icon;
    public final boolean isGame;
    public boolean isAudioBlocked;

    public AppInfo(String packageName, String appName, Drawable icon, boolean isGame, boolean isAudioBlocked) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isGame = isGame;
        this.isAudioBlocked = isAudioBlocked;
    }
}
