package com.youyouboydragon.soundblocker;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

public class ExclusiveFocusManager {
    private final AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean isHoldingFocus = false;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        // フォーカス変更イベント
    };

    public ExclusiveFocusManager(Context context) {
        this.audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @SuppressWarnings("deprecation")
    public synchronized boolean requestExclusiveFocus() {
        if (audioManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build();

            int result = audioManager.requestAudioFocus(focusRequest);
            isHoldingFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        } else {
            int result = audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            );
            isHoldingFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return isHoldingFocus;
    }

    @SuppressWarnings("deprecation")
    public synchronized void releaseFocus() {
        if (audioManager == null || !isHoldingFocus) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
                focusRequest = null;
            }
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        isHoldingFocus = false;
    }

    public boolean isHoldingFocus() {
        return isHoldingFocus;
    }
}
