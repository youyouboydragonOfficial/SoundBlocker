package com.youyouboydragon.soundblocker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.app.Activity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SoundBlockerNotificationService.OnSessionsChangedListener {

    private static final String GITHUB_REPO = "youyouboydragonOfficial/SoundBlocker";
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private LinearLayout activeSessionsContainer;
    private TextView permissionStatusText;
    private TextView updateStatusText;
    private ExclusiveFocusManager focusManager;
    private AudioManager audioManager;
    private Switch focusSwitch;
    private Switch floatingSwitch;
    private SeekBar volumeBar;

    private Dialog updateLoadingDialog;
    private Dialog downloadDialog;
    private SmoothProgressBar downloadBar;
    private TextView downloadPercentText;
    private TextView downloadSizeText;
    private ImageView downloadIconAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        focusManager = new ExclusiveFocusManager(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        buildUi();
    }

    private int dp(float val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable cardBg(int bgColor, int strokeColor, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xff0b0d17);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(40));

        // 1. Header
        TextView tag = new TextView(this);
        tag.setText("AUDIO SEPARATION & BLOCKER");
        tag.setTextSize(11);
        tag.setTextColor(0xff7c67fe);
        tag.setLetterSpacing(0.12f);
        tag.setTypeface(null, Typeface.BOLD);
        root.addView(tag, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("SoundBlocker");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(6));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("複数のアプリで音が鳴った時に、任意のアプリの音・音楽を個別に完全ブロック");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xff9ca3af);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(20)));

        // 2. Permission Card
        buildPermissionCard(root);

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(16)));

        // 3. Active Streams Card
        buildActiveStreamsCard(root);

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(16)));

        // 4. Quick Audio Control Card
        buildQuickControlCard(root);

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(16)));

        // 5. System Limitation Explanation Card
        buildExplanationCard(root);

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(16)));

        // 6. In-App Updater Card
        buildUpdaterCard(root);

        // Space
        root.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(24)));

        TextView footer = new TextView(this);
        footer.setText("© 2026 youyouboydragonOfficial  •  SoundBlocker");
        footer.setTextSize(11);
        footer.setTextColor(0xff4b5563);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void buildPermissionCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg(0xff151828, 0xff282c48, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView header = new TextView(this);
        header.setText("動作権限ステータス");
        header.setTextColor(0xff7c67fe);
        header.setTextSize(13);
        header.setTypeface(null, Typeface.BOLD);
        card.addView(header);

        permissionStatusText = new TextView(this);
        permissionStatusText.setTextSize(13);
        permissionStatusText.setPadding(0, dp(8), 0, dp(12));
        card.addView(permissionStatusText);

        Button grantBtn = new Button(this);
        grantBtn.setText("権限を設定する (通知アクセス・重ねて表示)");
        grantBtn.setTextColor(Color.WHITE);
        grantBtn.setTextSize(13);
        grantBtn.setAllCaps(false);
        grantBtn.setBackground(cardBg(0xff7c67fe, 0xff7c67fe, 12));
        grantBtn.setOnClickListener(v -> openPermissionSettings());
        card.addView(grantBtn, new LinearLayout.LayoutParams(-1, dp(44)));

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildActiveStreamsCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg(0xff151828, 0xff282c48, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView head = new TextView(this);
        head.setText("現在再生中のアプリ (個別ブロック)");
        head.setTextColor(Color.WHITE);
        head.setTextSize(15);
        head.setTypeface(null, Typeface.BOLD);
        headRow.addView(head, new LinearLayout.LayoutParams(0, -2, 1));

        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("更新 ⟳");
        refreshBtn.setTextColor(0xff7c67fe);
        refreshBtn.setTextSize(13);
        refreshBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        refreshBtn.setOnClickListener(v -> refreshSessionsList());
        headRow.addView(refreshBtn, new LinearLayout.LayoutParams(-2, -2));

        card.addView(headRow);

        TextView desc = new TextView(this);
        desc.setText("YouTube、Spotify、ブラウザ等の音声をアプリ単位で停止/ブロックできます。");
        desc.setTextColor(0xff9ca3af);
        desc.setTextSize(12);
        desc.setPadding(0, dp(4), 0, dp(12));
        card.addView(desc);

        activeSessionsContainer = new LinearLayout(this);
        activeSessionsContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(activeSessionsContainer, new LinearLayout.LayoutParams(-1, -2));

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildQuickControlCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg(0xff151828, 0xff282c48, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView head = new TextView(this);
        head.setText("クイック・オーディオ制御");
        head.setTextColor(Color.WHITE);
        head.setTextSize(15);
        head.setTypeface(null, Typeface.BOLD);
        card.addView(head);

        // 1. Floating Widget Switch
        LinearLayout floatRow = new LinearLayout(this);
        floatRow.setOrientation(LinearLayout.HORIZONTAL);
        floatRow.setGravity(Gravity.CENTER_VERTICAL);
        floatRow.setPadding(0, dp(12), 0, dp(12));

        LinearLayout floatInfo = new LinearLayout(this);
        floatInfo.setOrientation(LinearLayout.VERTICAL);
        TextView floatTitle = new TextView(this);
        floatTitle.setText("画面上のフローティング・ブロックボタン");
        floatTitle.setTextColor(Color.WHITE);
        floatTitle.setTextSize(14);
        floatInfo.addView(floatTitle);
        TextView floatDesc = new TextView(this);
        floatDesc.setText("別アプリやゲームをプレイ中も画面端から即ブロック");
        floatDesc.setTextColor(0xff9ca3af);
        floatDesc.setTextSize(11);
        floatInfo.addView(floatDesc);
        floatRow.addView(floatInfo, new LinearLayout.LayoutParams(0, -2, 1));

        floatingSwitch = new Switch(this);
        floatingSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    floatingSwitch.setChecked(false);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    startService(new Intent(this, SoundBlockerFloatingService.class));
                }
            } else {
                stopService(new Intent(this, SoundBlockerFloatingService.class));
            }
        });
        floatRow.addView(floatingSwitch, new LinearLayout.LayoutParams(-2, -2));
        card.addView(floatRow);

        // Divider
        View div1 = new View(this);
        div1.setBackgroundColor(0xff282c48);
        card.addView(div1, new LinearLayout.LayoutParams(-1, dp(1)));

        // 2. Exclusive Focus Switch
        LinearLayout focusRow = new LinearLayout(this);
        focusRow.setOrientation(LinearLayout.HORIZONTAL);
        focusRow.setGravity(Gravity.CENTER_VERTICAL);
        focusRow.setPadding(0, dp(12), 0, dp(12));

        LinearLayout focusInfo = new LinearLayout(this);
        focusInfo.setOrientation(LinearLayout.VERTICAL);
        TextView focusTitle = new TextView(this);
        focusTitle.setText("全バックグラウンド音声の強制ミュート (Focus)");
        focusTitle.setTextColor(Color.WHITE);
        focusTitle.setTextSize(14);
        focusInfo.addView(focusTitle);
        TextView focusDesc = new TextView(this);
        focusDesc.setText("オーディオフォーカスを独占し他アプリの音を停止");
        focusDesc.setTextColor(0xff9ca3af);
        focusDesc.setTextSize(11);
        focusInfo.addView(focusDesc);
        focusRow.addView(focusInfo, new LinearLayout.LayoutParams(0, -2, 1));

        focusSwitch = new Switch(this);
        focusSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                focusManager.requestExclusiveFocus();
            } else {
                focusManager.releaseFocus();
            }
        });
        focusRow.addView(focusSwitch, new LinearLayout.LayoutParams(-2, -2));
        card.addView(focusRow);

        // Divider
        View div2 = new View(this);
        div2.setBackgroundColor(0xff282c48);
        card.addView(div2, new LinearLayout.LayoutParams(-1, dp(1)));

        // 3. Media Volume Slider
        LinearLayout volRow = new LinearLayout(this);
        volRow.setOrientation(LinearLayout.VERTICAL);
        volRow.setPadding(0, dp(12), 0, 0);

        TextView volTitle = new TextView(this);
        volTitle.setText("システム・メディアマスター音量");
        volTitle.setTextColor(Color.WHITE);
        volTitle.setTextSize(14);
        volRow.addView(volTitle);

        volumeBar = new SeekBar(this);
        if (audioManager != null) {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeBar.setMax(max);
            volumeBar.setProgress(cur);
            volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && audioManager != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        volRow.addView(volumeBar, new LinearLayout.LayoutParams(-1, dp(36)));
        card.addView(volRow);

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildExplanationCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg(0xff121522, 0xff282c48, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView head = new TextView(this);
        head.setText("ℹ️ Androidの仕様と本アプリの仕組み");
        head.setTextColor(0xff9ca3af);
        head.setTextSize(13);
        head.setTypeface(null, Typeface.BOLD);
        card.addView(head);

        TextView exp = new TextView(this);
        exp.setText("■ なぜAndroidにはアプリ別の音量スライダーがないのか？\n" +
                "Android OSの標準仕様（AudioFlinger）では、セキュリティ上のサンドボックス設計により、一般アプリが他アプリのオーディオパイプラインを直接書き換えることは禁止されています（Samsung GalaxyのSoundAssistant等の特殊OS拡張を除く）。\n\n" +
                "■ 本アプリが音をブロックできる理由\n" +
                "本アプリはAndroidの「MediaSessionManager」および「オーディオフォーカス規約」と連携し、再生中のメディアアプリに対して直接バックグラウンド停止コマンドを発行することで、不要な音のブロック（消音）を実現しています。");
        exp.setTextColor(0xff6b7280);
        exp.setTextSize(11);
        exp.setLineSpacing(dp(2), 1.1f);
        exp.setPadding(0, dp(8), 0, 0);
        card.addView(exp);

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildUpdaterCard(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg(0xff151828, 0xff282c48, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView head = new TextView(this);
        head.setText("アプリ内更新 (In-App Updater)");
        head.setTextColor(Color.WHITE);
        head.setTextSize(15);
        head.setTypeface(null, Typeface.BOLD);
        card.addView(head);

        String currentVer = getAppVersion();
        updateStatusText = new TextView(this);
        updateStatusText.setText("現在のバージョン: " + currentVer + "\nGitHub Releasesから直接最新版にアップデートできます。");
        updateStatusText.setTextColor(0xff9ca3af);
        updateStatusText.setTextSize(12);
        updateStatusText.setPadding(0, dp(6), 0, dp(12));
        card.addView(updateStatusText);

        Button checkBtn = new Button(this);
        checkBtn.setText("アップデートを確認する");
        checkBtn.setTextColor(Color.WHITE);
        checkBtn.setTextSize(13);
        checkBtn.setAllCaps(false);
        checkBtn.setBackground(cardBg(0xff222538, 0xff7c67fe, 12));
        checkBtn.setOnClickListener(v -> checkForUpdate());
        card.addView(checkBtn, new LinearLayout.LayoutParams(-1, dp(44)));

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private void openPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkg = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && TextUtils.equals(pkg, cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        refreshSessionsList();
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.addListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            service.removeListener(this);
        }
    }

    private void updatePermissionStatus() {
        boolean notif = isNotificationServiceEnabled();
        boolean overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);

        if (notif && overlay) {
            permissionStatusText.setText("✓ すべての権限が正常に許可されています (準備完了)");
            permissionStatusText.setTextColor(0xff10b981);
        } else {
            StringBuilder sb = new StringBuilder("⚠️ 必要な権限が不足しています:\n");
            if (!notif) sb.append("• 通知アクセス権限 (音声セッションの制御)\n");
            if (!overlay) sb.append("• 他のアプリの上に重ねて表示 (フローティング操作)\n");
            permissionStatusText.setText(sb.toString().trim());
            permissionStatusText.setTextColor(0xffffb703);
        }
    }

    private void refreshSessionsList() {
        SoundBlockerNotificationService service = SoundBlockerNotificationService.getInstance();
        if (service != null) {
            onSessionsChanged(service.getActiveAudioSessions());
        } else {
            showEmptySessions();
        }
    }

    @Override
    public void onSessionsChanged(List<AudioSessionInfo> sessions) {
        runOnUiThread(() -> {
            if (activeSessionsContainer == null) return;
            activeSessionsContainer.removeAllViews();

            if (sessions == null || sessions.isEmpty()) {
                showEmptySessions();
                return;
            }

            for (AudioSessionInfo info : sessions) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(cardBg(0xff1a1d30, 0xff2e3455, 12));
                row.setPadding(dp(12), dp(10), dp(12), dp(10));

                ImageView icon = new ImageView(this);
                if (info.appIcon != null) {
                    icon.setImageDrawable(info.appIcon);
                } else {
                    icon.setImageResource(R.drawable.ic_launcher_foreground);
                }
                row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

                LinearLayout infoBox = new LinearLayout(this);
                infoBox.setOrientation(LinearLayout.VERTICAL);
                infoBox.setPadding(dp(12), 0, dp(12), 0);

                TextView name = new TextView(this);
                name.setText(info.appName);
                name.setTextColor(Color.WHITE);
                name.setTextSize(14);
                name.setTypeface(null, Typeface.BOLD);
                name.setSingleLine(true);
                infoBox.addView(name);

                TextView track = new TextView(this);
                String stateText = info.isPlaying ? "▶ 再生中" : "⏸ 一時停止中";
                if (!info.title.isEmpty()) {
                    track.setText(stateText + " • " + info.title);
                } else {
                    track.setText(stateText);
                }
                track.setTextColor(info.isPlaying ? 0xff10b981 : 0xff9ca3af);
                track.setTextSize(12);
                track.setSingleLine(true);
                infoBox.addView(track);

                row.addView(infoBox, new LinearLayout.LayoutParams(0, -2, 1));

                TextView btn = new TextView(this);
                boolean blocked = info.isBlocked;
                btn.setText(blocked ? "ブロック解除" : "音をブロック");
                btn.setTextSize(12);
                btn.setTypeface(null, Typeface.BOLD);
                btn.setTextColor(Color.WHITE);
                btn.setGravity(Gravity.CENTER);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(8));
                bg.setColor(blocked ? 0xff10b981 : 0xffff4d6d);
                btn.setBackground(bg);
                btn.setPadding(dp(12), dp(8), dp(12), dp(8));

                btn.setOnClickListener(v -> {
                    SoundBlockerNotificationService s = SoundBlockerNotificationService.getInstance();
                    if (s != null) {
                        if (blocked) {
                            s.unblockApp(info.packageName);
                        } else {
                            s.blockApp(info.packageName);
                        }
                    }
                });

                row.addView(btn, new LinearLayout.LayoutParams(-2, -2));

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.bottomMargin = dp(8);
                activeSessionsContainer.addView(row, rowLp);
            }
        });
    }

    private void showEmptySessions() {
        if (activeSessionsContainer == null) return;
        activeSessionsContainer.removeAllViews();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(16), 0, dp(16));

        TextView empty = new TextView(this);
        empty.setText("現在再生中のメディアセッションはありません");
        empty.setTextColor(0xff6b7280);
        empty.setTextSize(13);
        empty.setGravity(Gravity.CENTER);
        box.addView(empty);

        TextView hint = new TextView(this);
        hint.setText("YouTube、Spotify、音楽アプリ、ブラウザで動画や音声を再生すると自動で検出されます。");
        hint.setTextColor(0xff4b5563);
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(4), 0, 0);
        box.addView(hint);

        activeSessionsContainer.addView(box, new LinearLayout.LayoutParams(-1, -2));
    }

    // =========================================================================
    //  In-App Updater Implementation (アプリ内更新機能)
    // =========================================================================

    private void checkForUpdate() {
        showUpdateLoading();
        new Thread(() -> {
            long started = System.currentTimeMillis();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_LATEST).openConnection();
                conn.setRequestProperty("User-Agent", "SoundBlockerApp");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);

                if (conn.getResponseCode() != 200) {
                    throw new Exception("HTTP " + conn.getResponseCode());
                }

                InputStream in = conn.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                in.close();

                JSONObject release = new JSONObject(out.toString("UTF-8"));
                String tag = release.optString("tag_name", "");
                String body = release.optString("body", "新機能の追加および安定性の向上。");
                String apkUrl = null;
                long apkSize = 0;

                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url");
                            apkSize = asset.optLong("size", 0);
                            break;
                        }
                    }
                }

                String current = getAppVersion();
                boolean isNewer = compareVersion(tag.replace("v", ""), current) > 0;
                final String finalDownloadUrl = apkUrl;
                final long finalDownloadSize = apkSize;
                final String finalTag = tag;
                final String finalBody = body;

                // 必ず3秒以上読み込むこと（早すぎるとおかしく思われる）
                waitForLoading(started, 3200);

                runOnUiThread(() -> {
                    finishUpdateLoading();
                    if (isNewer && finalDownloadUrl != null) {
                        showUpdateDetailDialog(finalTag, current, finalBody, finalDownloadUrl, finalDownloadSize);
                    } else {
                        showUpToDateDialog(current);
                    }
                });
            } catch (Exception e) {
                // エラー時も最低3秒待機を保証
                waitForLoading(started, 3200);
                runOnUiThread(() -> {
                    finishUpdateLoading();
                    showUpdateFailedDialog();
                });
            }
        }).start();
    }

    private void waitForLoading(long started, long minDurationMs) {
        long elapsed = System.currentTimeMillis() - started;
        long rest = minDurationMs - elapsed;
        if (rest > 0) {
            try {
                Thread.sleep(rest);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void showUpdateLoading() {
        if (updateLoadingDialog != null && updateLoadingDialog.isShowing()) return;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(28), dp(28), dp(28), dp(28));
        box.setBackground(cardBg(0xff151828, 0xff7c67fe, 20));

        TextView over = new TextView(this);
        over.setText("CHECKING UPDATE");
        over.setTextSize(11);
        over.setTextColor(0xff7c67fe);
        over.setLetterSpacing(0.18f);
        over.setTypeface(null, Typeface.BOLD);
        box.addView(over);

        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminate(true);
        box.addView(spinner, new LinearLayout.LayoutParams(dp(44), dp(48)));

        TextView title = new TextView(this);
        title.setText("サーバーと通信中...");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("最新リリース情報を照合しています");
        sub.setTextColor(0xff9ca3af);
        sub.setTextSize(12);
        sub.setPadding(0, dp(4), 0, 0);
        box.addView(sub);

        updateLoadingDialog = new Dialog(this);
        updateLoadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        updateLoadingDialog.setContentView(box);
        updateLoadingDialog.setCancelable(false);
        updateLoadingDialog.show();

        if (updateLoadingDialog.getWindow() != null) {
            updateLoadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            updateLoadingDialog.getWindow().setDimAmount(0.75f);
            updateLoadingDialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85f), -2);
        }
    }

    private void finishUpdateLoading() {
        if (updateLoadingDialog != null && updateLoadingDialog.isShowing()) {
            updateLoadingDialog.dismiss();
        }
    }

    private void showUpdateDetailDialog(String newTag, String currentVer, String body, String downloadUrl, long downloadSize) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackground(cardBg(0xff151828, 0xff7c67fe, 20));

        TextView over = new TextView(this);
        over.setText("NEW VERSION AVAILABLE");
        over.setTextSize(11);
        over.setTextColor(0xff10b981);
        over.setLetterSpacing(0.15f);
        over.setTypeface(null, Typeface.BOLD);
        box.addView(over);

        TextView h = new TextView(this);
        h.setText("アップデートが見つかりました！");
        h.setTextSize(18);
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, dp(4), 0, dp(12));
        box.addView(h);

        // Version info bar
        LinearLayout verBar = new LinearLayout(this);
        verBar.setOrientation(LinearLayout.HORIZONTAL);
        verBar.setBackground(cardBg(0xff1f233a, 0xff2e3455, 10));
        verBar.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView curText = new TextView(this);
        curText.setText("現在: " + currentVer);
        curText.setTextColor(0xff9ca3af);
        curText.setTextSize(12);
        verBar.addView(curText, new LinearLayout.LayoutParams(0, -2, 1));

        TextView newText = new TextView(this);
        newText.setText("最新: " + newTag);
        newText.setTextColor(0xff10b981);
        newText.setTextSize(12);
        newText.setTypeface(null, Typeface.BOLD);
        verBar.addView(newText);
        box.addView(verBar);

        // Next update content details (次回の更新内容の詳細)
        TextView detailHead = new TextView(this);
        detailHead.setText("次回の更新内容の詳細:");
        detailHead.setTextColor(0xffd1d5db);
        detailHead.setTextSize(13);
        detailHead.setTypeface(null, Typeface.BOLD);
        detailHead.setPadding(0, dp(14), 0, dp(6));
        box.addView(detailHead);

        ScrollView contentScroll = new ScrollView(this);
        LinearLayout noteBox = new LinearLayout(this);
        noteBox.setOrientation(LinearLayout.VERTICAL);
        noteBox.setBackground(cardBg(0xff10121d, 0xff282c48, 8));
        noteBox.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView bodyText = new TextView(this);
        bodyText.setText(body);
        bodyText.setTextColor(0xff9ca3af);
        bodyText.setTextSize(12);
        bodyText.setLineSpacing(dp(2), 1.15f);
        noteBox.addView(bodyText);
        contentScroll.addView(noteBox);

        box.addView(contentScroll, new LinearLayout.LayoutParams(-1, dp(120)));

        if (downloadSize > 0) {
            TextView sizeText = new TextView(this);
            sizeText.setText("ダウンロードサイズ: 約 " + formatFileSize(downloadSize));
            sizeText.setTextColor(0xff6b7280);
            sizeText.setTextSize(11);
            sizeText.setPadding(0, dp(8), 0, dp(14));
            box.addView(sizeText);
        } else {
            box.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(14)));
        }

        // Actions
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("後で");
        cancelBtn.setTextColor(0xff9ca3af);
        cancelBtn.setBackground(null);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        actions.addView(cancelBtn);

        Button downloadBtn = new Button(this);
        downloadBtn.setText("今すぐ更新する");
        downloadBtn.setTextColor(Color.WHITE);
        downloadBtn.setTypeface(null, Typeface.BOLD);
        downloadBtn.setBackground(cardBg(0xff7c67fe, 0xff7c67fe, 12));
        downloadBtn.setOnClickListener(v -> {
            dialog.dismiss();
            downloadUpdate(downloadUrl, downloadSize);
        });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, dp(44));
        btnLp.leftMargin = dp(8);
        actions.addView(downloadBtn, btnLp);

        box.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        dialog.setContentView(box);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.75f);
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f), -2);
        }
    }

    private void showUpToDateDialog(String currentVer) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(26), dp(26), dp(26), dp(22));
        box.setBackground(cardBg(0xff151828, 0xff282c48, 20));

        TextView over = new TextView(this);
        over.setText("ALL CLEAR");
        over.setTextSize(11);
        over.setTextColor(0xff10b981);
        over.setLetterSpacing(0.18f);
        over.setTypeface(null, Typeface.BOLD);
        box.addView(over);

        TextView h = new TextView(this);
        h.setText("最新バージョンです");
        h.setTextSize(18);
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, dp(6), 0, dp(4));
        box.addView(h);

        TextView desc = new TextView(this);
        desc.setText("現在お使いのバージョン " + currentVer + " は最新です。\n更新の必要はありません。");
        desc.setTextColor(0xff9ca3af);
        desc.setTextSize(13);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, dp(18));
        box.addView(desc);

        Button okBtn = new Button(this);
        okBtn.setText("閉じる");
        okBtn.setTextColor(Color.WHITE);
        okBtn.setBackground(cardBg(0xff222538, 0xff2e3455, 12));
        okBtn.setOnClickListener(v -> dialog.dismiss());
        box.addView(okBtn, new LinearLayout.LayoutParams(dp(120), dp(42)));

        dialog.setContentView(box);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.75f);
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.82f), -2);
        }
    }

    private void showUpdateFailedDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(26), dp(26), dp(26), dp(22));
        box.setBackground(cardBg(0xff151828, 0xffff4d6d, 20));

        TextView over = new TextView(this);
        over.setText("CHECK FAILED");
        over.setTextSize(11);
        over.setTextColor(0xffff4d6d);
        over.setLetterSpacing(0.18f);
        over.setTypeface(null, Typeface.BOLD);
        box.addView(over);

        TextView h = new TextView(this);
        h.setText("更新情報を取得できませんでした");
        h.setTextSize(16);
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, dp(6), 0, dp(4));
        box.addView(h);

        TextView desc = new TextView(this);
        desc.setText("GitHub Releasesに接続できませんでした。\n通信状態をご確認のうえ、もう一度お試しください。");
        desc.setTextColor(0xff9ca3af);
        desc.setTextSize(12);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 0, 0, dp(18));
        box.addView(desc);

        Button okBtn = new Button(this);
        okBtn.setText("閉じる");
        okBtn.setTextColor(Color.WHITE);
        okBtn.setBackground(cardBg(0xff222538, 0xff2e3455, 12));
        okBtn.setOnClickListener(v -> dialog.dismiss());
        box.addView(okBtn, new LinearLayout.LayoutParams(dp(120), dp(42)));

        dialog.setContentView(box);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.75f);
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.82f), -2);
        }
    }

    private void showDownloadLoading() {
        if (downloadDialog != null && downloadDialog.isShowing()) return;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(24), dp(26), dp(24));
        box.setBackground(cardBg(0xff151828, 0xff7c67fe, 20));

        // Header with pulsing download animation
        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        downloadIconAnim = new ImageView(this);
        downloadIconAnim.setImageResource(R.drawable.ic_launcher_foreground);
        headRow.addView(downloadIconAnim, new LinearLayout.LayoutParams(dp(36), dp(36)));

        // Pulsing / rotation animation
        ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(downloadIconAnim, "scaleX", 1.0f, 1.15f, 1.0f);
        pulseAnim.setDuration(900);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.start();

        ObjectAnimator pulseAnimY = ObjectAnimator.ofFloat(downloadIconAnim, "scaleY", 1.0f, 1.15f, 1.0f);
        pulseAnimY.setDuration(900);
        pulseAnimY.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimY.start();

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);

        TextView over = new TextView(this);
        over.setText("DOWNLOADING UPDATE");
        over.setTextSize(11);
        over.setTextColor(0xff7c67fe);
        over.setLetterSpacing(0.12f);
        over.setTypeface(null, Typeface.BOLD);
        titleBox.addView(over);

        TextView h = new TextView(this);
        h.setText("新しいバージョンをダウンロード中");
        h.setTextSize(15);
        h.setTextColor(Color.WHITE);
        h.setTypeface(null, Typeface.BOLD);
        titleBox.addView(h);

        headRow.addView(titleBox);
        box.addView(headRow);

        box.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(18)));

        // Accurate Progress Bar (SmoothProgressBar)
        downloadBar = new SmoothProgressBar(this);
        box.addView(downloadBar, new LinearLayout.LayoutParams(-1, dp(12)));

        box.addView(new View(this), new LinearLayout.LayoutParams(-1, dp(10)));

        // Progress Text: percentage & size/speed
        LinearLayout textRow = new LinearLayout(this);
        textRow.setOrientation(LinearLayout.HORIZONTAL);

        downloadPercentText = new TextView(this);
        downloadPercentText.setText("0.0%");
        downloadPercentText.setTextColor(Color.WHITE);
        downloadPercentText.setTextSize(13);
        downloadPercentText.setTypeface(null, Typeface.BOLD);
        textRow.addView(downloadPercentText, new LinearLayout.LayoutParams(0, -2, 1));

        downloadSizeText = new TextView(this);
        downloadSizeText.setText("計算中...");
        downloadSizeText.setTextColor(0xff9ca3af);
        downloadSizeText.setTextSize(12);
        textRow.addView(downloadSizeText);

        box.addView(textRow);

        downloadDialog = new Dialog(this);
        downloadDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        downloadDialog.setContentView(box);
        downloadDialog.setCancelable(false);
        downloadDialog.show();

        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            downloadDialog.getWindow().setDimAmount(0.75f);
            downloadDialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f), -2);
        }
    }

    private void finishDownloadLoading() {
        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.dismiss();
        }
    }

    private void updateDownloadProgress(float percent, long currentBytes, long totalBytes, long speed) {
        if (downloadBar != null) {
            downloadBar.setProgressSmooth(percent);
        }
        if (downloadPercentText != null) {
            downloadPercentText.setText(String.format(Locale.US, "%.1f%%", percent));
        }
        if (downloadSizeText != null) {
            String speedStr = formatSpeed(speed);
            String prefix = speedStr.isEmpty() ? "" : speedStr + "  •  ";
            if (totalBytes > 0) {
                downloadSizeText.setText(prefix + formatFileSize(currentBytes) + " / " + formatFileSize(totalBytes));
            } else {
                downloadSizeText.setText(prefix + formatFileSize(currentBytes));
            }
        }
    }

    private void downloadUpdate(String downloadUrl, long expectedSize) {
        showDownloadLoading();
        new Thread(() -> {
            try {
                File dir = new File(getCacheDir(), "updates");
                if (!dir.exists()) dir.mkdirs();
                File apk = new File(dir, "SoundBlocker-update.apk");

                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setRequestProperty("User-Agent", "SoundBlockerApp");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);

                long total = conn.getContentLengthLong();
                if (total <= 0) total = expectedSize;
                final long totalBytes = total;

                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(apk);
                byte[] buf = new byte[16384];
                int n;
                long downloaded = 0;
                long lastUpdate = 0;
                long speedLastBytes = 0;
                long speedLastTime = System.currentTimeMillis();
                long currentSpeed = 0;

                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;
                    long now = System.currentTimeMillis();

                    if (now - speedLastTime >= 200) {
                        currentSpeed = (downloaded - speedLastBytes) * 1000 / Math.max(1, (now - speedLastTime));
                        speedLastBytes = downloaded;
                        speedLastTime = now;
                    }

                    if (now - lastUpdate > 16 || (totalBytes > 0 && downloaded >= totalBytes)) {
                        lastUpdate = now;
                        final float percent = totalBytes > 0 ? ((float) downloaded * 100f / (float) totalBytes) : 0f;
                        final long cur = downloaded;
                        final long spd = currentSpeed;
                        runOnUiThread(() -> updateDownloadProgress(percent, cur, totalBytes, spd));
                    }
                }
                in.close();
                out.close();

                final long finalTotal = totalBytes > 0 ? totalBytes : downloaded;
                final long finalSpd = currentSpeed;
                runOnUiThread(() -> updateDownloadProgress(100f, finalTotal, finalTotal, finalSpd));
                Thread.sleep(400);

                runOnUiThread(() -> {
                    finishDownloadLoading();
                    installApk(apk);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    finishDownloadLoading();
                    showUpdateFailedDialog();
                });
            }
        }).start();
    }

    private void installApk(File apk) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    Intent grantIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                    startActivity(grantIntent);
                }
            }
            Uri uri = Uri.parse("content://" + getPackageName() + ".fileprovider/update");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            updateStatusText.setText("インストーラーの起動に失敗しました: " + e.getMessage());
        }
    }

    private int compareVersion(String a, String b) {
        try {
            String[] x = a.split("\\.");
            String[] y = b.split("\\.");
            for (int i = 0; i < Math.max(x.length, y.length); i++) {
                int p = i < x.length ? Integer.parseInt(x[i]) : 0;
                int q = i < y.length ? Integer.parseInt(y[i]) : 0;
                if (p != q) return p > q ? 1 : -1;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024L * 1024L) return (bytes / 1024L) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String formatSpeed(long bytesPerSec) {
        if (bytesPerSec <= 0) return "";
        if (bytesPerSec < 1024L * 1024L) return (bytesPerSec / 1024L) + " KB/s";
        return String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0));
    }
}
