package com.youyouboydragon.soundblocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class SoundBlockerFloatingService extends Service implements SoundBlockerNotificationService.OnSessionsChangedListener {
    private static final String CHANNEL_ID = "soundblocker_floating_channel";
    private WindowManager windowManager;
    private View floatingBubble;
    private View expandedPanel;
    private LinearLayout panelItemsContainer;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private boolean isExpanded = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1001, createNotification());

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initBubbleView();
        initPanelView();

        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.addListener(this);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "フローティング音量ブロック",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("SoundBlocker 常駐中")
                .setContentText("画面上のバブルからアプリ音声をブロックできます")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    private int dp(float val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void initBubbleView() {
        FrameLayout bubble = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xee151828);
        bg.setStroke(dp(2), 0xff7c67fe);
        bubble.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher_foreground);
        int pad = dp(8);
        icon.setPadding(pad, pad, pad, pad);
        bubble.addView(icon, new FrameLayout.LayoutParams(dp(54), dp(54)));

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                dp(54),
                dp(54),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 20;
        bubbleParams.y = 300;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isClick = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isClick = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false;
                        }
                        bubbleParams.x = initialX + dx;
                        bubbleParams.y = initialY + dy;
                        windowManager.updateViewLayout(floatingBubble, bubbleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });

        floatingBubble = bubble;
        windowManager.addView(floatingBubble, bubbleParams);
    }

    private void initPanelView() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xf0151828);
        bg.setStroke(dp(1.5f), 0xff7c67fe);
        panel.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("SOUND BLOCKER");
        title.setTextColor(0xff7c67fe);
        title.setTextSize(13);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(0xff9ca3af);
        closeBtn.setTextSize(16);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(v -> hidePanel());
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));

        panel.addView(header, new LinearLayout.LayoutParams(-1, -2));

        panelItemsContainer = new LinearLayout(this);
        panelItemsContainer.setOrientation(LinearLayout.VERTICAL);
        panelItemsContainer.setPadding(0, dp(10), 0, 0);
        panel.addView(panelItemsContainer, new LinearLayout.LayoutParams(-1, -2));

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        panelParams = new WindowManager.LayoutParams(
                dp(280),
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.CENTER;

        expandedPanel = panel;
    }

    private void togglePanel() {
        if (isExpanded) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        if (!isExpanded && expandedPanel != null) {
            refreshPanelItems();
            windowManager.addView(expandedPanel, panelParams);
            isExpanded = true;
        }
    }

    private void hidePanel() {
        if (isExpanded && expandedPanel != null) {
            windowManager.removeView(expandedPanel);
            isExpanded = false;
        }
    }

    private void refreshPanelItems() {
        if (panelItemsContainer == null) return;
        panelItemsContainer.removeAllViews();

        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        List<AudioSessionInfo> sessions = service != null ? service.getActiveAudioSessions() : null;

        if (sessions == null || sessions.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("現在再生中のメディアはありません\n(YouTubeや音楽アプリを再生すると表示されます)");
            emptyText.setTextColor(0xff6b7280);
            emptyText.setTextSize(12);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dp(14), 0, dp(14));
            panelItemsContainer.addView(emptyText, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (AudioSessionInfo info : sessions) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            ImageView icon = new ImageView(this);
            if (info.appIcon != null) {
                icon.setImageDrawable(info.appIcon);
            } else {
                icon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            row.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));

            LinearLayout infoBox = new LinearLayout(this);
            infoBox.setOrientation(LinearLayout.VERTICAL);
            infoBox.setPadding(dp(8), 0, dp(8), 0);

            TextView name = new TextView(this);
            name.setText(info.appName);
            name.setTextColor(Color.WHITE);
            name.setTextSize(13);
            name.setSingleLine(true);
            infoBox.addView(name);

            TextView track = new TextView(this);
            track.setText(info.title.isEmpty() ? (info.isPlaying ? "再生中" : "一時停止中") : info.title);
            track.setTextColor(0xff9ca3af);
            track.setTextSize(11);
            track.setSingleLine(true);
            infoBox.addView(track);

            row.addView(infoBox, new LinearLayout.LayoutParams(0, -2, 1));

            TextView actionBtn = new TextView(this);
            boolean blocked = info.isBlocked;
            actionBtn.setText(blocked ? "解除" : "ブロック");
            actionBtn.setTextSize(12);
            actionBtn.setTextColor(Color.WHITE);
            actionBtn.setGravity(Gravity.CENTER);

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dp(8));
            btnBg.setColor(blocked ? 0xff10b981 : 0xffff4d6d);
            actionBtn.setBackground(btnBg);
            actionBtn.setPadding(dp(10), dp(6), dp(10), dp(6));

            actionBtn.setOnClickListener(v -> {
                if (service != null) {
                    if (blocked) {
                        service.unblockApp(info.packageName);
                    } else {
                        service.blockApp(info.packageName);
                    }
                    refreshPanelItems();
                }
            });

            row.addView(actionBtn, new LinearLayout.LayoutParams(-2, -2));
            panelItemsContainer.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    @Override
    public void onSessionsChanged(List<AudioSessionInfo> sessions) {
        if (isExpanded) {
            refreshPanelItems();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hidePanel();
        if (floatingBubble != null) {
            windowManager.removeView(floatingBubble);
        }
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.removeListener(this);
        }
    }
}
