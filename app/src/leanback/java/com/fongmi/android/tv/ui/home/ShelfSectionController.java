package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.ui.adapter.VodCardLandscapeAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitAdapter;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Shelves:
 * - "现在观看" (Watch Now - 16:9 Landscape compact cards, sunk to bottom of 1st screen)
 * - "继续观看" (Continue Watching - 16:9 Landscape cards with progress)
 * - "正在热播" (Hot Picks - 5-column 2:3 Portrait cards)
 *
 * Implements deterministic D-Pad step jumping, smooth center-focused scrolling,
 * and memory of focused card positions.
 */
public class ShelfSectionController {

    private final Activity mActivity;
    private final ActivityHomeBinding mBinding;
    private final ShelfCallback mCallback;

    private VodCardLandscapeAdapter mWatchNowAdapter;
    private VodCardLandscapeAdapter mContinueAdapter;
    private VodCardPortraitAdapter mHotPicksAdapter;

    private int mWatchNowFocusedPos = 0;
    private int mContinueFocusedPos = 0;
    private int mHotPicksFocusedPos = 0;

    public interface ShelfCallback {
        void onVodFocused(Vod vod);
        void onVodClicked(Vod vod);
        void onVodLongClicked(Vod vod);
        void onHistoryFocused(History history);
        void onHistoryClicked(History history);
        void onHistoryLongClicked(History history);
        void onNavigateToTopNav();
        void onOpenDrawer();
    }

    public ShelfSectionController(Activity activity, ActivityHomeBinding binding, ShelfCallback callback) {
        this.mActivity = activity;
        this.mBinding = binding;
        this.mCallback = callback;
        initViews();
    }

    private void initViews() {
        // Shelf 1: 现在观看 (Watch Now)
        mWatchNowAdapter = new VodCardLandscapeAdapter(new VodCardLandscapeAdapter.OnItemClickListener() {
            @Override
            public void onItemFocused(Object item) {
                if (item instanceof Vod vod && mCallback != null) {
                    mCallback.onVodFocused(vod);
                } else if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryFocused(hist);
                }
            }

            @Override
            public void onItemClicked(Object item) {
                if (item instanceof Vod vod && mCallback != null) {
                    mCallback.onVodClicked(vod);
                } else if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryClicked(hist);
                }
            }

            @Override
            public void onItemLongClicked(Object item) {
                if (item instanceof Vod vod && mCallback != null) {
                    mCallback.onVodLongClicked(vod);
                } else if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryLongClicked(hist);
                }
            }
        });
        mBinding.recyclerWatchNow.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        mBinding.recyclerWatchNow.setAdapter(mWatchNowAdapter);
        setupWatchNowKeyNavigation();

        // Shelf 2: 继续观看 (Continue Watching)
        mContinueAdapter = new VodCardLandscapeAdapter(new VodCardLandscapeAdapter.OnItemClickListener() {
            @Override
            public void onItemFocused(Object item) {
                if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryFocused(hist);
                }
            }

            @Override
            public void onItemClicked(Object item) {
                if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryClicked(hist);
                }
            }

            @Override
            public void onItemLongClicked(Object item) {
                if (item instanceof History hist && mCallback != null) {
                    mCallback.onHistoryLongClicked(hist);
                }
            }
        });
        mBinding.recyclerContinue.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        mBinding.recyclerContinue.setAdapter(mContinueAdapter);
        setupContinueKeyNavigation();

        // Shelf 3: 正在热播 (Hot Picks 5 columns)
        mHotPicksAdapter = new VodCardPortraitAdapter(new VodCardPortraitAdapter.OnVodClickListener() {
            @Override
            public void onVodFocused(Vod vod) {
                if (mCallback != null) mCallback.onVodFocused(vod);
            }

            @Override
            public void onVodClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodClicked(vod);
            }

            @Override
            public void onVodLongClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodLongClicked(vod);
            }
        });
        mBinding.gridHot.setLayoutManager(new GridLayoutManager(mActivity, 5));
        mBinding.gridHot.setAdapter(mHotPicksAdapter);
        setupHotPicksKeyNavigation();
    }

    private void setupWatchNowKeyNavigation() {
        mBinding.recyclerWatchNow.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.recyclerWatchNow.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) {
                        mWatchNowFocusedPos = pos;
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        jumpToNextShelfFromWatchNow();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        mBinding.homeScrollView.smoothScrollTo(0, 0);
                        if (mCallback != null) {
                            mCallback.onNavigateToTopNav();
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                        if (mCallback != null) {
                            mCallback.onOpenDrawer();
                        }
                        return true;
                    }
                    return false;
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                view.setOnKeyListener(null);
            }
        });
    }

    private void setupContinueKeyNavigation() {
        mBinding.recyclerContinue.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.recyclerContinue.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) {
                        mContinueFocusedPos = pos;
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        jumpToWatchNowFromContinue();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        jumpToHotPicksFromContinue();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                        if (mCallback != null) {
                            mCallback.onOpenDrawer();
                        }
                        return true;
                    }
                    return false;
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                view.setOnKeyListener(null);
            }
        });
    }

    private void setupHotPicksKeyNavigation() {
        mBinding.gridHot.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.gridHot.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) {
                        mHotPicksFocusedPos = pos;
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (pos < 5) { // In the first row of hot picks
                            jumpUpFromHotPicks();
                            return true;
                        } else {
                            smoothScrollGridRow(v, -1);
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        smoothScrollGridRow(v, 1);
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos % 5 == 0 && pos < 5) {
                        if (mCallback != null) {
                            mCallback.onOpenDrawer();
                        }
                        return true;
                    }
                    return false;
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                view.setOnKeyListener(null);
            }
        });
    }

    private void jumpToNextShelfFromWatchNow() {
        if (isContinueWatchingVisible()) {
            int targetPos = Math.min(mContinueFocusedPos, mContinueAdapter.getItemCount() - 1);
            if (targetPos < 0) targetPos = 0;
            int targetY = Math.max(0, mBinding.headerContinue.getTop() - ResUtil.dp2px(70));
            mBinding.homeScrollView.smoothScrollTo(0, targetY);
            focusContinueItem(targetPos);
        } else {
            jumpToHotPicksDirect();
        }
    }

    private void jumpToWatchNowFromContinue() {
        mBinding.homeScrollView.smoothScrollTo(0, 0);
        int targetPos = Math.min(mWatchNowFocusedPos, mWatchNowAdapter.getItemCount() - 1);
        if (targetPos < 0) targetPos = 0;
        focusWatchNowItem(targetPos);
    }

    private void jumpToHotPicksFromContinue() {
        int targetY = Math.max(0, mBinding.headerHot.getTop() - ResUtil.dp2px(70));
        mBinding.homeScrollView.smoothScrollTo(0, targetY);
        int targetPos = Math.min(mHotPicksFocusedPos % 5, mHotPicksAdapter.getItemCount() - 1);
        if (targetPos < 0) targetPos = 0;
        focusHotPicksItem(targetPos);
    }

    private void jumpToHotPicksDirect() {
        int targetY = Math.max(0, mBinding.headerHot.getTop() - ResUtil.dp2px(70));
        mBinding.homeScrollView.smoothScrollTo(0, targetY);
        int targetPos = Math.min(mHotPicksFocusedPos % 5, mHotPicksAdapter.getItemCount() - 1);
        if (targetPos < 0) targetPos = 0;
        focusHotPicksItem(targetPos);
    }

    private void jumpUpFromHotPicks() {
        if (isContinueWatchingVisible()) {
            int targetPos = Math.min(mContinueFocusedPos, mContinueAdapter.getItemCount() - 1);
            if (targetPos < 0) targetPos = 0;
            int targetY = Math.max(0, mBinding.headerContinue.getTop() - ResUtil.dp2px(70));
            mBinding.homeScrollView.smoothScrollTo(0, targetY);
            focusContinueItem(targetPos);
        } else {
            mBinding.homeScrollView.smoothScrollTo(0, 0);
            int targetPos = Math.min(mWatchNowFocusedPos, mWatchNowAdapter.getItemCount() - 1);
            if (targetPos < 0) targetPos = 0;
            focusWatchNowItem(targetPos);
        }
    }

    private void smoothScrollGridRow(View currentView, int direction) {
        App.post(() -> {
            View focused = mBinding.gridHot.getFocusedChild();
            if (focused != null) {
                int scrollY = mBinding.headerHot.getTop() + focused.getTop() - ResUtil.dp2px(120);
                mBinding.homeScrollView.smoothScrollTo(0, Math.max(0, scrollY));
            }
        }, 80);
    }

    public void focusWatchNowItem(int position) {
        App.post(() -> {
            mBinding.recyclerWatchNow.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.recyclerWatchNow.findViewHolderForAdapterPosition(position);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else if (mBinding.recyclerWatchNow.getChildCount() > 0) {
                    mBinding.recyclerWatchNow.getChildAt(0).requestFocus();
                }
            }, 50);
        }, 50);
    }

    public void focusContinueItem(int position) {
        App.post(() -> {
            mBinding.recyclerContinue.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.recyclerContinue.findViewHolderForAdapterPosition(position);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else if (mBinding.recyclerContinue.getChildCount() > 0) {
                    mBinding.recyclerContinue.getChildAt(0).requestFocus();
                }
            }, 50);
        }, 50);
    }

    public void focusHotPicksItem(int position) {
        App.post(() -> {
            mBinding.gridHot.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.gridHot.findViewHolderForAdapterPosition(position);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else if (mBinding.gridHot.getChildCount() > 0) {
                    mBinding.gridHot.getChildAt(0).requestFocus();
                }
            }, 50);
        }, 50);
    }

    public boolean isContinueWatchingVisible() {
        return mBinding.headerContinue.getVisibility() == View.VISIBLE && mContinueAdapter.getItemCount() > 0;
    }

    public void setWatchNowData(List<Vod> items) {
        mWatchNowAdapter.setItems(items);
    }

    public void setContinueData(List<History> items) {
        if (items != null && !items.isEmpty()) {
            mBinding.headerContinue.setVisibility(View.VISIBLE);
            mBinding.recyclerContinue.setVisibility(View.VISIBLE);
            mContinueAdapter.setItems(items);
        } else {
            mBinding.headerContinue.setVisibility(View.GONE);
            mBinding.recyclerContinue.setVisibility(View.GONE);
            mContinueAdapter.setItems(new ArrayList<>());
        }
    }

    public void setHotPicksData(List<Vod> items) {
        mHotPicksAdapter.setItems(items);
    }

    public boolean isWatchNowEmpty() {
        return mWatchNowAdapter.isEmpty();
    }

    public void clear() {
        mWatchNowAdapter.setItems(new ArrayList<>());
        mContinueAdapter.setItems(new ArrayList<>());
        mHotPicksAdapter.setItems(new ArrayList<>());
    }
}
