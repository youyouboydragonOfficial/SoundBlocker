package com.youyouboydragon.soundblocker;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundBlockerNotificationService extends NotificationListenerService {
    private static SoundBlockerNotificationService instance;
    private MediaSessionManager mediaSessionManager;
    private ComponentName serviceComponent;
    private final Set<String> blockedPackages = new HashSet<>();
    private final List<OnSessionsChangedListener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnSessionsChangedListener {
        void onSessionsChanged(List<AudioSessionInfo> sessions);
    }

    public static SoundBlockerNotificationService getInstance() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        serviceComponent = new ComponentName(this, SoundBlockerNotificationService.class);

        try {
            if (mediaSessionManager != null) {
                mediaSessionManager.addOnActiveSessionsChangedListener(controllers -> {
                    notifySessionsChanged();
                }, serviceComponent);
            }
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        notifySessionsChanged();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        notifySessionsChanged();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        notifySessionsChanged();
    }

    public void addListener(OnSessionsChangedListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
        notifySessionsChanged();
    }

    public void removeListener(OnSessionsChangedListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifySessionsChanged() {
        mainHandler.post(() -> {
            List<AudioSessionInfo> list = getActiveAudioSessions();
            synchronized (listeners) {
                for (OnSessionsChangedListener l : listeners) {
                    l.onSessionsChanged(list);
                }
            }
        });
    }

    public List<AudioSessionInfo> getActiveAudioSessions() {
        List<AudioSessionInfo> result = new ArrayList<>();
        if (mediaSessionManager == null) return result;

        PackageManager pm = getPackageManager();
        try {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(serviceComponent);
            if (controllers != null) {
                for (MediaController c : controllers) {
                    String pkg = c.getPackageName();
                    if (pkg.equals(getPackageName())) continue; // 自分自身は除外

                    String appName = pkg;
                    Drawable icon = null;
                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                        appName = pm.getApplicationLabel(appInfo).toString();
                        icon = pm.getApplicationIcon(appInfo);
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }

                    MediaMetadata metadata = c.getMetadata();
                    String title = "";
                    String artist = "";
                    if (metadata != null) {
                        title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                        artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                    }
                    if (title == null || title.isEmpty()) {
                        title = appName + " (オーディオ)";
                    }

                    PlaybackState state = c.getPlaybackState();
                    boolean isPlaying = false;
                    if (state != null) {
                        isPlaying = state.getState() == PlaybackState.STATE_PLAYING;
                    }

                    boolean isBlocked = blockedPackages.contains(pkg);
                    result.add(new AudioSessionInfo(pkg, appName, title, artist != null ? artist : "", isPlaying, isBlocked, icon));
                }
            }
        } catch (SecurityException ignored) {
        }
        return result;
    }

    public void blockApp(String packageName) {
        blockedPackages.add(packageName);
        if (mediaSessionManager != null) {
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(serviceComponent);
                if (controllers != null) {
                    for (MediaController c : controllers) {
                        if (c.getPackageName().equals(packageName)) {
                            MediaController.TransportControls controls = c.getTransportControls();
                            if (controls != null) {
                                controls.pause();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        notifySessionsChanged();
    }

    public void unblockApp(String packageName) {
        blockedPackages.remove(packageName);
        if (mediaSessionManager != null) {
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(serviceComponent);
                if (controllers != null) {
                    for (MediaController c : controllers) {
                        if (c.getPackageName().equals(packageName)) {
                            MediaController.TransportControls controls = c.getTransportControls();
                            if (controls != null) {
                                controls.play();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        notifySessionsChanged();
    }

    public boolean isAppBlocked(String packageName) {
        return blockedPackages.contains(packageName);
    }
}
