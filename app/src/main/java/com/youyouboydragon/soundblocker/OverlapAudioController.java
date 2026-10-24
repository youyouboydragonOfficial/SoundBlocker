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
     * ゲーム音だけを鳴らしたい場合（YouTube・音楽の音を完全ブロック）
     */
    public void muteMediaOnly() {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                service.blockApp(s.packageName);
            }
        }
        // フォーカスを解放してゲームに音を出させる
        focusManager.releaseFocus();
    }

    /**
     * YouTube・音楽だけを聴きたい場合（ゲームの音をブロック）
     */
    public void muteGameOnly() {
        // 1. YouTubeなどのメディアセッションのブロックを解除（再生再開）
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            List<AudioSessionInfo> sessions = service.getActiveAudioSessions();
            for (AudioSessionInfo s : sessions) {
                service.unblockApp(s.packageName);
            }
        }
        // 2. 登録済みゲームアプリの音声をブロック
        for (String pkg : appOpsBlocker.getBlockedPackages()) {
            appOpsBlocker.setAppAudioBlocked(pkg, true);
        }
    }

    /**
     * すべてのブロックを解除（ゲームもYouTubeも両方鳴らす）
     */
    public void unmuteAll() {
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
}
