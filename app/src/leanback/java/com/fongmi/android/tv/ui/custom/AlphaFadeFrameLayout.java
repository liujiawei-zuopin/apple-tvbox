package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Apple TV Style Alpha Fade FrameLayout.
 * Applies a smooth vertical hardware-accelerated alpha gradient mask (PorterDuff.Mode.DST_IN)
 * so that child views (Hero poster and masks) seamlessly dissolve into 100% transparency at the bottom,
 * naturally exposing the underlying full-screen multi-spectral Gaussian frosted ambient backdrop without seams.
 */
public class AlphaFadeFrameLayout extends FrameLayout {

    private Paint mMaskPaint;
    private LinearGradient mGradient;
    private int mLastW = -1;
    private int mLastH = -1;

    public AlphaFadeFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public AlphaFadeFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlphaFadeFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && (w != mLastW || h != mLastH)) {
            mLastW = w;
            mLastH = h;
            // 4-stop smooth cubic-like alpha attenuation curve:
            // 0% ~ 40%: 100% Solid Opacity (Crisp poster subject)
            // 40% ~ 68%: Gentle initial fade (Alpha ~217)
            // 68% ~ 88%: Progressive feathering (Alpha ~90)
            // 88% ~ 100%: Complete dissolve into 0 (Alpha 0 at bottom edge)
            mGradient = new LinearGradient(
                    0, 0, 0, h,
                    new int[]{
                            0xFF000000,
                            0xFF000000,
                            0xD9000000,
                            0x5A000000,
                            0x00000000
                    },
                    new float[]{0f, 0.40f, 0.68f, 0.88f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            mMaskPaint.setShader(mGradient);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mGradient == null || getWidth() <= 0 || getHeight() <= 0) {
            super.dispatchDraw(canvas);
            return;
        }

        // Offscreen layer for hardware-accelerated PorterDuff compositing
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        super.dispatchDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mMaskPaint);
        canvas.restoreToCount(saveCount);
    }
}
