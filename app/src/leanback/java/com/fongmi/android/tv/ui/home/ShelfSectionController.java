package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.ui.adapter.VodCardLandscapeAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitAdapter;
import com.fongmi.android.tv.ui.custom.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Shelves:
 * - "正在观看" (Watch Now - 16:9 Landscape compact cards)
 * - "继续观看" (Continue Watching - 16:9 Landscape cards with progress)
 * - "热门推荐" (Hot Picks - 5-column 2:3 Portrait cards)
 *
 * Decoupled from Hero area and FongMi data providers to ensure rock-solid focus navigation.
 */
public class ShelfSectionController {

    private final Activity mActivity;
    private final ActivityHomeBinding mBinding;
    private final ShelfCallback mCallback;

    private VodCardLandscapeAdapter mWatchNowAdapter;
    private VodCardLandscapeAdapter mContinueAdapter;
    private VodCardPortraitAdapter mHotPicksAdapter;

    public interface ShelfCallback {
        void onVodFocused(Vod vod);
        void onVodClicked(Vod vod);
        void onVodLongClicked(Vod vod);
        void onHistoryFocused(History history);
        void onHistoryClicked(History history);
        void onHistoryLongClicked(History history);
    }

    public ShelfSectionController(Activity activity, ActivityHomeBinding binding, ShelfCallback callback) {
        this.mActivity = activity;
        this.mBinding = binding;
        this.mCallback = callback;
        initViews();
    }

    private void initViews() {
        // Shelf 1: 正在观看 (Watch Now)
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

        // Shelf 3: 热门推荐 (5 columns)
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
