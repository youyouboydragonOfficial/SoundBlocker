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
     * 端末マスター音量は一切触らず、対象アプリ単体の音声出力をOSレベルで遮断(AppOps)
     * 同時にMediaSessionの一時停止も選択的に適用可能
     */
    public boolean muteAppSoundOnly(String packageName) {
        // 全体の音量は絶対に触らない！対象アプリのPLAY_AUDIOのみ遮断
        return appOpsBlocker.setAppAudioBlocked(packageName, true);
    }

    /**
     * アプリ単体の消音を解除
     */
    public boolean unmuteAppSoundOnly(String packageName) {
        return appOpsBlocker.setAppAudioBlocked(packageName, false);
    }

    /**
     * アプリの再生を一時停止 (Pause)
     */
    public void pauseApp(String packageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.pauseApp(packageName);
        }
    }

    /**
     * アプリの再生を再開 (Play)
     */
    public void resumeApp(String packageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.resumeApp(packageName);
        }
    }

    /**
     * すべての消音・停止を解除
     */
    public void unblockAll() {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                service.resumeApp(s.packageName);
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
