package com.youyouboydragon.soundblocker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class SmoothProgressBar extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float currentProgress = 0f;
    private float targetProgress = 0f;
    private ValueAnimator progAnim;
    private ValueAnimator shimmerAnim;
    private float shimmerPhase = 0f;

    public SmoothProgressBar(Context context) {
        super(context);
        init();
    }

    public SmoothProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        shimmerAnim = ValueAnimator.ofFloat(0f, 1f);
        shimmerAnim.setDuration(1200);
        shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnim.addUpdateListener(v -> {
            shimmerPhase = (float) v.getAnimatedValue();
            invalidate();
        });
        shimmerAnim.start();
    }

    public void setProgressSmooth(float target) {
        if (target < 0) target = 0;
        if (target > 100) target = 100;
        targetProgress = target;

        if (progAnim != null) {
            progAnim.cancel();
        }
        progAnim = ValueAnimator.ofFloat(currentProgress, targetProgress);
        progAnim.setDuration(150);
        progAnim.setInterpolator(new DecelerateInterpolator());
        progAnim.addUpdateListener(v -> {
            currentProgress = (float) v.getAnimatedValue();
            invalidate();
        });
        progAnim.start();
    }

    public float getCurrentProgress() {
        return currentProgress;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (progAnim != null) progAnim.cancel();
        if (shimmerAnim != null) shimmerAnim.cancel();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float radius = h / 2f;

        // 1. 背景トラック
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff151828);
        rect.set(0, 0, w, h);
        c.drawRoundRect(rect, radius, radius, paint);

        // トラック枠線
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xff2b314f);
        c.drawRoundRect(rect, radius, radius, paint);

        // 2. 進行バー (グラデーション バイオレット〜エメラルド)
        float fillW = w * (currentProgress / 100f);
        if (fillW > 0) {
            c.save();
            rect.set(0, 0, w, h);
            Path clip = new Path();
            clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
            c.clipPath(clip);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff7c67fe);
            rect.set(0, 0, fillW, h);
            c.drawRect(rect, paint);

            // 3. シマーアニメーション
            float shimmerW = dp(60);
            float shimmerX = -shimmerW + shimmerPhase * (fillW + shimmerW * 2);
            LinearGradient gradient = new LinearGradient(
                    shimmerX, 0, shimmerX + shimmerW, 0,
                    new int[]{0x00ffffff, 0x55ffffff, 0x00ffffff},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(gradient);
            rect.set(shimmerX, 0, shimmerX + shimmerW, h);
            c.drawRect(rect, paint);
            paint.setShader(null);

            c.restore();
        }
    }

    private float dp(float value) {
        return value * getContext().getResources().getDisplayMetrics().density;
    }
}
