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
    private LinearLayout dynamicSelectorContainer;
    private LinearLayout panelItemsContainer;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private boolean isExpanded = false;
    private OverlapAudioController overlapController;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        overlapController = new OverlapAudioController(this);
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
        return builder.setContentTitle("SoundBlocker 動作中")
                .setContentText("再生を止めずに音だけを選択ブロック")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    private int dp(float val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable btnBg(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
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
        bg.setColor(0xf4151828);
        bg.setStroke(dp(1.5f), 0xff7c67fe);
        panel.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("SOUND BLOCKER  /  音消音");
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

        // Note: 再生は止めず音だけブロック
        TextView note = new TextView(this);
        note.setText("※ 再生を止めずに、音（音声出力）だけを完全消音します");
        note.setTextColor(0xff9ca3af);
        note.setTextSize(10);
        note.setPadding(0, dp(4), 0, dp(6));
        panel.addView(note);

        // Dynamic Selector
        dynamicSelectorContainer = new LinearLayout(this);
        dynamicSelectorContainer.setOrientation(LinearLayout.VERTICAL);
        panel.addView(dynamicSelectorContainer, new LinearLayout.LayoutParams(-1, -2));

        // Individual App List
        panelItemsContainer = new LinearLayout(this);
        panelItemsContainer.setOrientation(LinearLayout.VERTICAL);
        panel.addView(panelItemsContainer, new LinearLayout.LayoutParams(-1, -2));

        // Unblock All Button
        TextView btnUnmuteAll = new TextView(this);
        btnUnmuteAll.setText("🔄 すべての音を鳴らす (ブロック全解除)");
        btnUnmuteAll.setTextColor(0xff10b981);
        btnUnmuteAll.setTextSize(11);
        btnUnmuteAll.setGravity(Gravity.CENTER);
        btnUnmuteAll.setBackground(btnBg(0xff162822, 6));
        btnUnmuteAll.setPadding(0, dp(8), 0, dp(8));
        btnUnmuteAll.setOnClickListener(v -> {
            overlapController.unblockAll();
            refreshPanelItems();
        });
        LinearLayout.LayoutParams unmuteLp = new LinearLayout.LayoutParams(-1, -2);
        unmuteLp.topMargin = dp(8);
        panel.addView(btnUnmuteAll, unmuteLp);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        panelParams = new WindowManager.LayoutParams(
                dp(300),
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
        if (panelItemsContainer == null || dynamicSelectorContainer == null) return;
        panelItemsContainer.removeAllViews();
        dynamicSelectorContainer.removeAllViews();

        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        List<AudioSessionInfo> sessions = service != null ? service.getActiveAudioSessions() : null;

        // 1. 動的選択ボタン群の生成 (複数アプリ検出時)
        if (sessions != null && sessions.size() >= 2) {
            TextView prompt = new TextView(this);
            prompt.setText("複数のアプリで音が鳴っています。消音するアプリを選択:");
            prompt.setTextColor(0xffc4b5fd);
            prompt.setTextSize(11);
            prompt.setPadding(0, 0, 0, dp(6));
            dynamicSelectorContainer.addView(prompt);

            for (AudioSessionInfo s : sessions) {
                TextView selectBtn = new TextView(this);
                selectBtn.setText("🔇 「" + s.appName + "」の音だけ消す (再生は継続)");
                selectBtn.setTextColor(Color.WHITE);
                selectBtn.setTextSize(12);
                selectBtn.setGravity(Gravity.CENTER);
                selectBtn.setBackground(btnBg(0xff432658, 8));
                selectBtn.setPadding(dp(8), dp(7), dp(8), dp(7));
                selectBtn.setOnClickListener(v -> {
                    overlapController.muteAppSound(s.packageName);
                    refreshPanelItems();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(4);
                dynamicSelectorContainer.addView(selectBtn, lp);
            }

            View div = new View(this);
            div.setBackgroundColor(0xff282c48);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, dp(1));
            divLp.topMargin = dp(4);
            divLp.bottomMargin = dp(8);
            dynamicSelectorContainer.addView(div, divLp);
        }

        // 2. アプリ個別リストの生成
        if (sessions == null || sessions.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("現在再生中のオーディオアプリはありません\n(アプリで音が出ると自動表示されます)");
            emptyText.setTextColor(0xff6b7280);
            emptyText.setTextSize(11);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dp(12), 0, dp(8));
            panelItemsContainer.addView(emptyText, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (AudioSessionInfo info : sessions) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));

            ImageView icon = new ImageView(this);
            if (info.appIcon != null) {
                icon.setImageDrawable(info.appIcon);
            } else {
                icon.setImageResource(R.drawable.ic_launcher_foreground);
            }
            row.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));

            LinearLayout infoBox = new LinearLayout(this);
            infoBox.setOrientation(LinearLayout.VERTICAL);
            infoBox.setPadding(dp(8), 0, dp(8), 0);

            TextView name = new TextView(this);
            name.setText(info.appName);
            name.setTextColor(Color.WHITE);
            name.setTextSize(12);
            name.setSingleLine(true);
            infoBox.addView(name);

            TextView track = new TextView(this);
            boolean blocked = info.isBlocked || overlapController.isAppBlocked(info.packageName);
            String stateStr = blocked ? "🔇 音声消音中 (再生継続)" : (info.isPlaying ? "▶ 再生中" : "⏸ 一時停止中");
            track.setText(info.title.isEmpty() ? stateStr : (stateStr + " • " + info.title));
            track.setTextColor(blocked ? 0xffff4d6d : 0xff9ca3af);
            track.setTextSize(10);
            track.setSingleLine(true);
            infoBox.addView(track);

            row.addView(infoBox, new LinearLayout.LayoutParams(0, -2, 1));

            // 音だけ消音ボタン
            TextView actionBtn = new TextView(this);
            actionBtn.setText(blocked ? "消音解除" : "音だけ消音");
            actionBtn.setTextSize(11);
            actionBtn.setTextColor(Color.WHITE);
            actionBtn.setGravity(Gravity.CENTER);
            actionBtn.setBackground(btnBg(blocked ? 0xff10b981 : 0xffff4d6d, 6));
            actionBtn.setPadding(dp(8), dp(4), dp(8), dp(4));

            actionBtn.setOnClickListener(v -> {
                if (blocked) {
                    overlapController.unmuteAppSound(info.packageName);
                } else {
                    overlapController.muteAppSound(info.packageName);
                }
                refreshPanelItems();
            });
            row.addView(actionBtn, new LinearLayout.LayoutParams(-2, -2));

            // 一時停止(Pause)用サブボタン
            if (service != null && info.isPlaying) {
                TextView pauseBtn = new TextView(this);
                pauseBtn.setText("⏸");
                pauseBtn.setTextSize(12);
                pauseBtn.setTextColor(0xff9ca3af);
                pauseBtn.setGravity(Gravity.CENTER);
                pauseBtn.setPadding(dp(6), dp(4), dp(2), dp(4));
                pauseBtn.setOnClickListener(v -> {
                    service.pauseApp(info.packageName);
                    refreshPanelItems();
                });
                row.addView(pauseBtn, new LinearLayout.LayoutParams(-2, -2));
            }

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
