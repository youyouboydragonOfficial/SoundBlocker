package com.youyouboydragon.soundblocker;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class AppOpsAudioBlocker {

    private static final String PREF_NAME = "sound_blocker_apps";
    private static final String KEY_BLOCKED_PACKAGES = "blocked_packages";
    private static final int OP_PLAY_AUDIO = 28; // AppOpsManager.OP_PLAY_AUDIO

    private final Context context;
    private final SharedPreferences prefs;
    private final Set<String> blockedPackages;

    public AppOpsAudioBlocker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.blockedPackages = new HashSet<>(prefs.getStringSet(KEY_BLOCKED_PACKAGES, new HashSet<>()));
    }

    public boolean isAppBlocked(String packageName) {
        return blockedPackages.contains(packageName);
    }

    public boolean setAppAudioBlocked(String packageName, boolean block) {
        if (block) {
            blockedPackages.add(packageName);
        } else {
            blockedPackages.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, new HashSet<>(blockedPackages)).apply();

        // 1. Shizuku / Root / シェルコマンドによる直接反映を試みる
        boolean success = runShellAppOps(packageName, block);
        return success;
    }

    private boolean runShellAppOps(String packageName, boolean block) {
        String mode = block ? "ignore" : "allow";
        String cmd = "cmd appops set " + packageName + " PLAY_AUDIO " + mode;

        try {
            // su (root) または 既存の権限付きシェルでの実行を試行
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            int code = p.waitFor();
            if (code == 0) return true;
        } catch (Exception ignored) {
        }

        try {
            // 通常のshまたはrishでの実行を試行
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            int code = p.waitFor();
            if (code == 0) return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public String getAdbCommand(String packageName, boolean block) {
        String mode = block ? "ignore" : "allow";
        return "adb shell cmd appops set " + packageName + " PLAY_AUDIO " + mode;
    }

    public Set<String> getBlockedPackages() {
        return new HashSet<>(blockedPackages);
    }
}
