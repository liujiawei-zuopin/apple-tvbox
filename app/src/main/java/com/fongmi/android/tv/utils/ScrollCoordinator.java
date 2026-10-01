package com.fongmi.android.tv.utils;

import android.view.View;

/**
 * Apple TV Style Unified Scroll & Motion Coordinator.
 * Coordinates top bar translation/alpha collapse, hero info fade out,
 * and parallax depth effects across Home, Category Channels, and Search pages.
 */
public class ScrollCoordinator {

    private static final int MAX_COLLAPSE_DP = 52;
    private static final int TOP_BAR_SCROLL_RANGE_DP = 65;
    private static final int HERO_FADE_RANGE_DP = 160;

    /**
     * Unified scroll response handler.
     * @param scrollY Current scroll offset in pixels
     * @param topBar Top navigation bar view
     * @param heroInfo Hero title/synopsis/play group view
     * @param heroDots Hero carousel indicator dots view
     */
    public static void onScroll(int scrollY, View topBar, View heroInfo, View heroDots) {
        if (topBar != null) {
            float progress = Math.min(1.0f, Math.max(0f, (float) scrollY / ResUtil.dp2px(TOP_BAR_SCROLL_RANGE_DP)));
            topBar.setTranslationY(-progress * ResUtil.dp2px(MAX_COLLAPSE_DP));
            topBar.setAlpha(1.0f - progress * 0.85f);
        }

        if (heroInfo != null || heroDots != null) {
            float alpha = Math.max(0f, 1.0f - (float) scrollY / ResUtil.dp2px(HERO_FADE_RANGE_DP));
            if (heroInfo != null) heroInfo.setAlpha(alpha);
            if (heroDots != null) heroDots.setAlpha(alpha);
        }
    }

    public static void resetTopBar(View topBar) {
        if (topBar != null) {
            topBar.setTranslationY(0);
            topBar.setAlpha(1.0f);
        }
    }

    public static void animateRestoreTopBar(View topBar) {
        if (topBar != null) {
            topBar.animate().translationY(0).alpha(1.0f).setDuration(150).start();
        }
    }
}
