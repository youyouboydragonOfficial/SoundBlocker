package com.youyouboydragon.soundblocker;

import android.content.Context;
import java.util.List;

public class OverlapAudioController {

    private final Context context;
    private final AppOpsAudioBlocker appOpsBlocker;
    private final ExclusiveFocusManager focusManager;

    public OverlapAudioController(Context context) {
        this.context = context.getApplicationContext();
        this.appOpsBlocker = new AppOpsAudioBlocker(context);
        this.focusManager = new ExclusiveFocusManager(context);
    }

    public AppOpsAudioBlocker getAppOpsBlocker() {
        return appOpsBlocker;
    }

    public ExclusiveFocusManager getFocusManager() {
        return focusManager;
    }

    /**
     * 再生を停止（Pause）させず、音（音声出力）だけを完全にブロック（消音）する
     */
    public void muteAppSound(String packageName) {
        // 1. メディアセッションを持つ音楽アプリ等の音量を0/ミュート（再生は継続）
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.muteAppSound(packageName);
        }
        // 2. OSレベルで音声波形出力を遮断（PLAY_AUDIO ignore / 再生は継続）
        appOpsBlocker.setAppAudioBlocked(packageName, true);
    }

    /**
     * 音声の消音（ブロック）を解除する
     */
    public void unmuteAppSound(String packageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.unmuteAppSound(packageName);
        }
        appOpsBlocker.setAppAudioBlocked(packageName, false);
    }

    /**
     * 指定したアプリの音だけを鳴らし、それ以外のアプリの音を消音（再生は継続）
     */
    public void keepOnlyApp(String keepPackageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                if (!s.packageName.equals(keepPackageName)) {
                    muteAppSound(s.packageName);
                } else {
                    unmuteAppSound(s.packageName);
                }
            }
        }
        for (String pkg : appOpsBlocker.getBlockedPackages()) {
            if (!pkg.equals(keepPackageName)) {
                muteAppSound(pkg);
            }
        }
    }

    /**
     * すべての消音を解除（すべてのアプリの音を鳴らす）
     */
    public void unblockAll() {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                service.unmuteAppSound(s.packageName);
            }
        }
        for (String pkg : appOpsBlocker.getBlockedPackages()) {
            appOpsBlocker.setAppAudioBlocked(pkg, false);
        }
        focusManager.releaseFocus();
    }

    public boolean isAppBlocked(String packageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null && service.isAppBlocked(packageName)) {
            return true;
        }
        return appOpsBlocker.isAppBlocked(packageName);
    }
}
