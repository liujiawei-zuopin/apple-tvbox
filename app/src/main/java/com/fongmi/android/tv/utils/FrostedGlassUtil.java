package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * Generates true Gaussian frosted glass ambient backdrops (深色磨砂毛玻璃背景).
 * Renders multi-point organic radial light orbs onto a downscaled canvas
 * and diffuses them with large-radius StackBlur, creating rich, velvety,
 * luminous frosted glass ambient fields (Emerald, Sapphire, Amethyst).
 */
public class FrostedGlassUtil {

    private static final int TARGET_W = 320;
    private static final int TARGET_H = 180;
    private static final int BLUR_RADIUS = 26;

    private static Bitmap sEmerald;
    private static Bitmap sSapphire;
    private static Bitmap sAmethyst;
    private static Bitmap sDefault;

    public static synchronized Bitmap getEmeraldFrosted() {
        if (sEmerald == null || sEmerald.isRecycled()) {
            sEmerald = createAmbient(
                    0xFF08160F,
                    0x6000E599, 0x25059669,
                    0x4500C853, 0x15047857,
                    0x3500897B
            );
        }
        return sEmerald;
    }

    public static synchronized Bitmap getSapphireFrosted() {
        if (sSapphire == null || sSapphire.isRecycled()) {
            sSapphire = createAmbient(
                    0xFF070E1A,
                    0x600A84FF, 0x251D4ED8,
                    0x451E88E5, 0x151E40AF,
                    0x351565C0
            );
        }
        return sSapphire;
    }

    public static synchronized Bitmap getAmethystFrosted() {
        if (sAmethyst == null || sAmethyst.isRecycled()) {
            sAmethyst = createAmbient(
                    0xFF120717,
                    0x60BF5AF2, 0x259333EA,
                    0x45AB47BC, 0x157B1FA2,
                    0x356A1B9A
            );
        }
        return sAmethyst;
    }

    public static synchronized Bitmap getDefaultFrosted() {
        if (sDefault == null || sDefault.isRecycled()) {
            sDefault = createAmbient(
                    0xFF101014,
                    0x303B4252, 0x152E3440,
                    0x254C566A, 0x102E3440,
                    0x203B4252
            );
        }
        return sDefault;
    }

    private static Bitmap createAmbient(int baseColor,
                                        int orb1Center, int orb1Edge,
                                        int orb2Center, int orb2Edge,
                                        int orb3Center) {
        Bitmap bitmap = Bitmap.createBitmap(TARGET_W, TARGET_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(baseColor);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Orb 1: Upper-Right vibrant ambient glow
        float x1 = TARGET_W * 0.72f;
        float y1 = TARGET_H * 0.28f;
        float r1 = TARGET_W * 0.58f;
        paint.setShader(new RadialGradient(
                x1, y1, r1,
                new int[]{orb1Center, orb1Edge, 0x00000000},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(x1, y1, r1, paint);

        // Orb 2: Mid-Left deep ambient glow
        float x2 = TARGET_W * 0.20f;
        float y2 = TARGET_H * 0.65f;
        float r2 = TARGET_W * 0.52f;
        paint.setShader(new RadialGradient(
                x2, y2, r2,
                new int[]{orb2Center, orb2Edge, 0x00000000},
                new float[]{0f, 0.60f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(x2, y2, r2, paint);

        // Orb 3: Bottom-Right subtle ambient depth
        float x3 = TARGET_W * 0.82f;
        float y3 = TARGET_H * 0.85f;
        float r3 = TARGET_W * 0.42f;
        paint.setShader(new RadialGradient(
                x3, y3, r3,
                new int[]{orb3Center, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(x3, y3, r3, paint);

        // Diffuse completely with large-radius StackBlur for buttery smooth frosted glass
        return BlurUtil.fastBlur(bitmap, BLUR_RADIUS, true);
    }
}
