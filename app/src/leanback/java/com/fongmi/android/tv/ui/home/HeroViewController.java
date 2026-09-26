package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Hero Backdrop, Title, Metadata and Synopsis presentation.
 * Isolated from shelf layout and data providers to ensure zero layout-recalculation thrashing.
 */
public class HeroViewController {

    private final Activity mActivity;
    private final ActivityHomeBinding mBinding;
    private Runnable mDebounceRunnable;

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
                Glide.with(mActivity)
                        .load(ImgUtil.getUrl(vod.getPic()))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(mBinding.heroBackdrop);
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

    public void setPlaceholder(String title, String meta, String desc) {
        mBinding.heroTitle.setText(title != null ? title : "");
        mBinding.heroMeta.setText(meta != null ? meta : "");
        mBinding.heroDesc.setText(desc != null ? desc : "");
        mBinding.heroDesc.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);
    }

    public void setVisibility(int visibility) {
        mBinding.heroInfoLayout.setVisibility(visibility);
    }

    public void destroy() {
        if (mDebounceRunnable != null) {
            App.removeCallbacks(mDebounceRunnable);
            mDebounceRunnable = null;
        }
    }
}
