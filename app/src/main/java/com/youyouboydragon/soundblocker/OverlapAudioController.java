package com.youyouboydragon.soundblocker;

import android.content.Context;
import java.util.List;
import java.util.Set;

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
     * 任意の特定アプリの音声をブロック（消音）
     */
    public void blockApp(String packageName) {
        // 1. メディアセッションを持つアプリならMediaControllerで一時停止
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.blockApp(packageName);
        }
        // 2. ゲームや一般アプリならAppOps/PLAY_AUDIOでブロック
        appOpsBlocker.setAppAudioBlocked(packageName, true);
    }

    /**
     * 任意の特定アプリの音声ブロックを解除（再生再開）
     */
    public void unblockApp(String packageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.unblockApp(packageName);
        }
        appOpsBlocker.setAppAudioBlocked(packageName, false);
    }

    /**
     * 指定したアプリの音だけを残し、それ以外の現在鳴っている他アプリの音をすべてブロックする
     */
    public void keepOnlyApp(String keepPackageName) {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                if (!s.packageName.equals(keepPackageName)) {
                    service.blockApp(s.packageName);
                } else {
                    service.unblockApp(s.packageName);
                }
            }
        }
        // 登録済みアプリの処理
        for (String pkg : appOpsBlocker.getBlockedPackages()) {
            if (!pkg.equals(keepPackageName)) {
                appOpsBlocker.setAppAudioBlocked(pkg, true);
            }
        }
    }

    /**
     * 全ての音声ブロックを解除（すべてのアプリの音を鳴らす）
     */
    public void unblockAll() {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                service.unblockApp(s.packageName);
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
