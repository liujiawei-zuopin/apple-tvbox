package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.LayoutSideDrawerBinding;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.FileActivity;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.PushActivity;
import com.fongmi.android.tv.ui.activity.SearchActivity;
import com.fongmi.android.tv.ui.activity.SettingActivity;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.ResUtil;

/**
 * Controller for Apple tvOS Side Drawer sliding panel and menu routing.
 */
public class SideDrawerController {

    private final Activity mActivity;
    private final LayoutSideDrawerBinding mBinding;
    private View mLastFocusedView;

    public SideDrawerController(Activity activity, LayoutSideDrawerBinding binding) {
        this.mActivity = activity;
        this.mBinding = binding;
        initEvents();
    }

    private void initEvents() {
        mBinding.menuSearch.setOnClickListener(v -> { closeDrawer(); SearchActivity.start(mActivity); });
        mBinding.menuHistory.setOnClickListener(v -> { closeDrawer(); CollectActivity.start(mActivity, mActivity.getString(R.string.home_history)); });
        mBinding.menuLive.setOnClickListener(v -> { closeDrawer(); LiveActivity.start(mActivity); });
        mBinding.menuConfig.setOnClickListener(v -> { closeDrawer(); ConfigDialog.create().vod().show(mActivity); });
        mBinding.menuSite.setOnClickListener(v -> { closeDrawer(); SiteDialog.create().show(mActivity); });
        mBinding.menuCloud.setOnClickListener(v -> { closeDrawer(); mActivity.startActivity(new Intent(mActivity, FileActivity.class)); });
        mBinding.menuCollect.setOnClickListener(v -> { closeDrawer(); KeepActivity.start(mActivity); });
        mBinding.menuPush.setOnClickListener(v -> { closeDrawer(); PushActivity.start(mActivity); });
        mBinding.menuSetting.setOnClickListener(v -> { closeDrawer(); SettingActivity.start(mActivity); });
        mBinding.drawerMask.setOnClickListener(v -> closeDrawer());

        View.OnKeyListener rightKeyListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                closeDrawer();
                return true;
            }
            return false;
        };
        mBinding.menuSearch.setOnKeyListener(rightKeyListener);
        mBinding.menuHistory.setOnKeyListener(rightKeyListener);
        mBinding.menuLive.setOnKeyListener(rightKeyListener);
        mBinding.menuConfig.setOnKeyListener(rightKeyListener);
        mBinding.menuSite.setOnKeyListener(rightKeyListener);
        mBinding.menuCloud.setOnKeyListener(rightKeyListener);
        mBinding.menuCollect.setOnKeyListener(rightKeyListener);
        mBinding.menuPush.setOnKeyListener(rightKeyListener);
        mBinding.menuSetting.setOnKeyListener(rightKeyListener);
    }

    public void openDrawer() {
        if (mBinding.drawerLayout.getVisibility() == View.VISIBLE) return;
        mLastFocusedView = mActivity.getCurrentFocus();
        mBinding.drawerLayout.setVisibility(View.VISIBLE);
        mBinding.drawerPanel.setTranslationX(-ResUtil.dp2px(320));
        mBinding.drawerPanel.animate().translationX(0).setDuration(220).start();
        mBinding.drawerMask.setAlpha(0f);
        mBinding.drawerMask.animate().alpha(1f).setDuration(220).start();
        mBinding.menuSearch.requestFocus();
    }

    public void closeDrawer() {
        if (mBinding.drawerLayout.getVisibility() != View.VISIBLE) return;
        mBinding.drawerPanel.animate().translationX(-ResUtil.dp2px(320)).setDuration(180).start();
        mBinding.drawerMask.animate().alpha(0f).setDuration(180).withEndAction(() -> {
            mBinding.drawerLayout.setVisibility(View.GONE);
            if (mLastFocusedView != null) {
                mLastFocusedView.requestFocus();
            }
        }).start();
    }

    public boolean isDrawerOpen() {
        return mBinding.drawerLayout.getVisibility() == View.VISIBLE;
    }

    public void toggleDrawer() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }
}
