package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.utils.BlurUtil;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Hero Backdrop, Title, Metadata and Synopsis presentation.
 * Features dual-layer backdrop with dynamic blur and parallax fade-out on vertical scrolling.
 */
public class HeroViewController {

    private final Activity mActivity;
    private final ActivityHomeBinding mBinding;
    private Runnable mDebounceRunnable;
    private float mCurrentScrollFraction = 0f;

    public HeroViewController(Activity activity, ActivityHomeBinding binding) {
        this.mActivity = activity;
        this.mBinding = binding;
    }

    public void updateHero(Vod vod) {
        if (vod == null) return;
        if (mDebounceRunnable != null) {
            App.removeCallbacks(mDebounceRunnable);
        }
        mDebounceRunnable = () -> {
            if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
            mBinding.heroTitle.setText(vod.getName() != null ? vod.getName() : "");

            List<String> metas = new ArrayList<>();
            if (!TextUtils.isEmpty(vod.getRemarks())) metas.add(vod.getRemarks());
            if (!TextUtils.isEmpty(vod.getYear())) metas.add(vod.getYear());
            if (!TextUtils.isEmpty(vod.getArea())) metas.add(vod.getArea());
            if (!TextUtils.isEmpty(vod.getDirector())) metas.add("导演: " + vod.getDirector());
            mBinding.heroMeta.setText(TextUtils.join(" · ", metas));

            String desc = vod.getContent();
            if (TextUtils.isEmpty(desc)) desc = vod.getActor();
            mBinding.heroDesc.setText(desc != null ? desc : "");
            mBinding.heroDesc.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);

            if (!TextUtils.isEmpty(vod.getPic())) {
                Object model = ImgUtil.getUrl(vod.getPic());
                // Layer 1: Crisp high-res backdrop
                Glide.with(mActivity)
                        .load(model)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(mBinding.heroBackdrop);

                // Layer 2: Pre-generated frosted blur backdrop for instant 60fps scrolling
                Glide.with(mActivity)
                        .asBitmap()
                        .load(model)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
                                Task.execute(() -> {
                                    Bitmap blurred = BlurUtil.blur(resource, 20, 4);
                                    if (blurred != null) {
                                        App.post(() -> {
                                            if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
                                            mBinding.heroBlurBackdrop.setImageBitmap(blurred);
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });
            }
        };
        App.post(mDebounceRunnable, 80);
    }

    public void updateHero(History history) {
        if (history == null) return;
        Vod v = new Vod();
        v.setName(history.getVodName());
        v.setPic(history.getVodPic());
        v.setRemarks(history.getVodRemarks());
        updateHero(v);
    }

    /**
     * Called whenever homeScrollView scrolls.
     * Gradually transforms clear backdrop into a deep frosted glass texture and fades hero text.
     */
    public void onScroll(int scrollY) {
        int threshold = ResUtil.dp2px(180);
        if (threshold <= 0) threshold = 1;
        float fraction = Math.max(0f, Math.min(1.0f, (float) scrollY / threshold));
        mCurrentScrollFraction = fraction;

        mBinding.heroBlurBackdrop.setAlpha(fraction);
        mBinding.heroFrostedOverlay.setAlpha(fraction * 0.88f);

        // Parallax fade out for hero synopsis text
        float textAlpha = Math.max(0f, 1.0f - (fraction * 1.3f));
        mBinding.heroInfoLayout.setAlpha(textAlpha);
    }

    public void resetScroll() {
        mCurrentScrollFraction = 0f;
        mBinding.heroBlurBackdrop.setAlpha(0f);
        mBinding.heroFrostedOverlay.setAlpha(0f);
        mBinding.heroInfoLayout.setAlpha(1.0f);
    }

    public void setPlaceholder(String title, String meta, String desc) {
        mBinding.heroTitle.setText(title != null ? title : "");
        mBinding.heroMeta.setText(meta != null ? meta : "");
        mBinding.heroDesc.setText(desc != null ? desc : "");
        mBinding.heroDesc.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);
    }

    public void setVisibility(int visibility) {
        mBinding.heroInfoLayout.setVisibility(visibility);
        mBinding.heroBackdrop.setVisibility(visibility);
        mBinding.heroBlurBackdrop.setVisibility(visibility);
        mBinding.heroFrostedOverlay.setVisibility(visibility);
        mBinding.heroScrim.setVisibility(visibility);
    }

    public void destroy() {
        if (mDebounceRunnable != null) {
            App.removeCallbacks(mDebounceRunnable);
            mDebounceRunnable = null;
        }
    }
}
