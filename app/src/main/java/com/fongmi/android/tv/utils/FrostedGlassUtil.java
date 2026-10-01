package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.SparseArray;

/**
 * Apple TV Multi-Spectral Gaussian Frosted Glass Ambient Theme Engine.
 * 
 * Generates and caches 12 distinct high-luxury organic dark ambient backdrops:
 * 1. Emerald (电影 / Film)
 * 2. Sapphire (剧集 / TV Drama)
 * 3. Amethyst (综艺 / Variety Show)
 * 4. Sakura (动漫 / 二次元 / Animation)
 * 5. Aurora Cyan (纪录片 / 探索 / Documentary)
 * 6. Flame Orange (体育 / 竞技 / Sports)
 * 7. Sun Gold (少儿 / 儿童 / Kids)
 * 8. Vintage Amber (经典 / 怀旧 / Vintage)
 * 9. Crimson Ruby (短剧 / 微剧 / Action)
 * 10. Cosmic Violet (科幻 / 奇幻 / Sci-Fi)
 * 11. Cyber Teal (科技 / 游戏 / Gaming)
 * 12. Deep Slate (默认 / 搜索 / Default)
 *
 * Supports semantic matching and hash-based deterministic fallback for any new channel.
 */
public class FrostedGlassUtil {

    private static final int TARGET_W = 320;
    private static final int TARGET_H = 180;
    private static final int BLUR_RADIUS = 26;

    public static final int THEME_EMERALD = 0;
    public static final int THEME_SAPPHIRE = 1;
    public static final int THEME_AMETHYST = 2;
    public static final int THEME_SAKURA = 3;
    public static final int THEME_AURORA_CYAN = 4;
    public static final int THEME_FLAME_ORANGE = 5;
    public static final int THEME_SUN_GOLD = 6;
    public static final int THEME_VINTAGE_AMBER = 7;
    public static final int THEME_CRIMSON_RUBY = 8;
    public static final int THEME_COSMIC_VIOLET = 9;
    public static final int THEME_CYBER_TEAL = 10;
    public static final int THEME_DEEP_SLATE = 11;
    public static final int THEME_COUNT = 12;

    private static final SparseArray<Bitmap> sCache = new SparseArray<>();

    public static synchronized Bitmap getThemeBitmap(int themeId) {
        Bitmap cached = sCache.get(themeId);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap created;
        switch (themeId) {
            case THEME_EMERALD:
                created = createAmbient(0xFF050E09, 0x3800E599, 0x15059669, 0x2600C853, 0x0D047857, 0x1E00897B);
                break;
            case THEME_SAPPHIRE:
                created = createAmbient(0xFF040811, 0x380A84FF, 0x151D4ED8, 0x261E88E5, 0x0D1E40AF, 0x1E1565C0);
                break;
            case THEME_AMETHYST:
                created = createAmbient(0xFF09040D, 0x38BF5AF2, 0x159333EA, 0x26AB47BC, 0x0D7B1FA2, 0x1E6A1B9A);
                break;
            case THEME_SAKURA:
                created = createAmbient(0xFF0D0408, 0x38FF375F, 0x15BE185D, 0x26E91E63, 0x0D9C27B0, 0x1EAD1457);
                break;
            case THEME_AURORA_CYAN:
                created = createAmbient(0xFF030D0B, 0x3830D158, 0x15059669, 0x2600BFA5, 0x0D00897B, 0x1E004D40);
                break;
            case THEME_FLAME_ORANGE:
                created = createAmbient(0xFF0D0703, 0x38FF9F0A, 0x15D97706, 0x26FF6D00, 0x0DE65100, 0x1EBF360C);
                break;
            case THEME_SUN_GOLD:
                created = createAmbient(0xFF0D0A03, 0x38FFD60A, 0x15B45309, 0x26FFAB00, 0x0DFF8F00, 0x1EF57F17);
                break;
            case THEME_VINTAGE_AMBER:
                created = createAmbient(0xFF0A0705, 0x38AC8E68, 0x1578350F, 0x268D6E63, 0x0D5D4037, 0x1E4E342E);
                break;
            case THEME_CRIMSON_RUBY:
                created = createAmbient(0xFF0D0405, 0x38FF453A, 0x15B91C1C, 0x26D50000, 0x0DB71C1C, 0x1E880E4F);
                break;
            case THEME_COSMIC_VIOLET:
                created = createAmbient(0xFF07040E, 0x385E5CE6, 0x154338CA, 0x26651FFF, 0x0D311B92, 0x1E1A237E);
                break;
            case THEME_CYBER_TEAL:
                created = createAmbient(0xFF030A0E, 0x3864D2FF, 0x150369A1, 0x260091EA, 0x0D01579B, 0x1E006064);
                break;
            case THEME_DEEP_SLATE:
            default:
                created = createAmbient(0xFF08080C, 0x253B4252, 0x102E3440, 0x1A4C566A, 0x0A2E3440, 0x143B4252);
                break;
        }

        sCache.put(themeId, created);
        return created;
    }

    /**
     * Resolves the spectral theme bitmap for any category (standard or dynamically added).
     */
    public static Bitmap getFrostedForCategory(String typeId, String typeName) {
        String combined = (typeId != null ? typeId : "") + " " + (typeName != null ? typeName : "");
        combined = combined.toLowerCase();

        // 1. Semantic Keyword Matching
        if (combined.contains("movie") || combined.contains("电影") || combined.contains("片") || combined.contains("影院")) {
            return getThemeBitmap(THEME_EMERALD);
        } else if (combined.contains("tv") || combined.contains("剧") || combined.contains("连续剧") || combined.contains("drama")) {
            return getThemeBitmap(THEME_SAPPHIRE);
        } else if (combined.contains("variety") || combined.contains("综艺") || combined.contains("show") || combined.contains("娱乐")) {
            return getThemeBitmap(THEME_AMETHYST);
        } else if (combined.contains("anime") || combined.contains("漫") || combined.contains("二次元") || combined.contains("番剧") || combined.contains("cartoon")) {
            return getThemeBitmap(THEME_SAKURA);
        } else if (combined.contains("doc") || combined.contains("纪") || combined.contains("自然") || combined.contains("探索") || combined.contains("地理")) {
            return getThemeBitmap(THEME_AURORA_CYAN);
        } else if (combined.contains("sport") || combined.contains("体") || combined.contains("球") || combined.contains("赛事") || combined.contains("竞技")) {
            return getThemeBitmap(THEME_FLAME_ORANGE);
        } else if (combined.contains("kid") || combined.contains("少儿") || combined.contains("儿童") || combined.contains("亲子") || combined.contains("益智")) {
            return getThemeBitmap(THEME_SUN_GOLD);
        } else if (combined.contains("vintage") || combined.contains("老") || combined.contains("经典") || combined.contains("怀旧") || combined.contains("classic")) {
            return getThemeBitmap(THEME_VINTAGE_AMBER);
        } else if (combined.contains("short") || combined.contains("短剧") || combined.contains("微剧") || combined.contains("热血")) {
            return getThemeBitmap(THEME_CRIMSON_RUBY);
        } else if (combined.contains("scifi") || combined.contains("科幻") || combined.contains("奇幻") || combined.contains("悬疑") || combined.contains("惊悚")) {
            return getThemeBitmap(THEME_COSMIC_VIOLET);
        } else if (combined.contains("tech") || combined.contains("科技") || combined.contains("游戏") || combined.contains("电竞") || combined.contains("game")) {
            return getThemeBitmap(THEME_CYBER_TEAL);
        }

        // 2. Hash-based deterministic fallback for unknown categories
        int hash = Math.abs(combined.hashCode());
        int fallbackTheme = hash % (THEME_COUNT - 1); // maps across 0 ~ 10
        return getThemeBitmap(fallbackTheme);
    }

    // Convenience backward-compatible accessors
    public static Bitmap getEmeraldFrosted() { return getThemeBitmap(THEME_EMERALD); }
    public static Bitmap getSapphireFrosted() { return getThemeBitmap(THEME_SAPPHIRE); }
    public static Bitmap getAmethystFrosted() { return getThemeBitmap(THEME_AMETHYST); }
    public static Bitmap getCategoryDefaultFrosted() { return getThemeBitmap(THEME_DEEP_SLATE); }
    public static Bitmap getDefaultFrosted() { return getThemeBitmap(THEME_DEEP_SLATE); }

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
