package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.LayoutCategoryChannelBinding;
import com.fongmi.android.tv.ui.adapter.FilterChipAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardLandscapeAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitShelfAdapter;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple TV+ rich category channels ("电影", "剧集", "综艺"):
 * 1. Top Section: Hero Banner Carousel (rotates hot titles with "▶ 立即播放" button).
 * 2. Shelf 1: "推荐" (16:9 Landscape cards, strictly decoupled from Hero).
 * 3. Shelf 2+: Sub-genre Shelves (2:3 Portrait cards).
 * 4. Section 4: "全部影片" (Sub-category filters + 5-column poster grid).
 */
public class CategoryViewController implements FilterChipAdapter.OnClickListener {

    private final Activity mActivity;
    private final LayoutCategoryChannelBinding mBinding;
    private final CategoryCallback mCallback;

    private VodCardLandscapeAdapter mRecommendAdapter;
    private VodCardPortraitShelfAdapter mSubGenre1Adapter;
    private VodCardPortraitShelfAdapter mSubGenre2Adapter;
    private VodCardPortraitAdapter mGridAdapter;
    private FilterChipAdapter mFilterAdapter;

    private final List<Vod> mHeroItems = new ArrayList<>();
    private int mHeroIndex = 0;
    private Vod mCurrentHeroVod;
    private Runnable mCarouselRunnable;

    private int mRecommendFocusedPos = 0;
    private int mSubGenre1FocusedPos = 0;
    private int mSubGenre2FocusedPos = 0;
    private int mGridFocusedPos = 0;

    public interface CategoryCallback {
        void onVodClicked(Vod vod);
        void onVodLongClicked(Vod vod);
        void onFilterSelected(Value value);
        void onNavigateToTopNav();
        void onOpenDrawer();
        void onCategoryScrolled(int scrollY);
    }

    public CategoryViewController(Activity activity, LayoutCategoryChannelBinding binding, CategoryCallback callback) {
        this.mActivity = activity;
        this.mBinding = binding;
        this.mCallback = callback;
        initViews();
    }

    private void initViews() {
        // 1. Hero Play Button
        mBinding.categoryBtnPlay.setOnClickListener(v -> {
            if (mCurrentHeroVod != null && mCallback != null) {
                mCallback.onVodClicked(mCurrentHeroVod);
            }
        });
        mBinding.categoryBtnPlay.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (mCallback != null) mCallback.onNavigateToTopNav();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                jumpToRecommendFromHero();
                return true;
            }
            return false;
        });

        // 2. Shelf 1: "推荐" (16:9 Landscape compact cards, does NOT alter Hero)
        mRecommendAdapter = new VodCardLandscapeAdapter(new VodCardLandscapeAdapter.OnItemClickListener() {
            @Override
            public void onItemFocused(Object item) {
                // Background stays untouched as requested
            }

            @Override
            public void onItemClicked(Object item) {
                if (item instanceof Vod vod && mCallback != null) {
                    mCallback.onVodClicked(vod);
                }
            }

            @Override
            public void onItemLongClicked(Object item) {
                if (item instanceof Vod vod && mCallback != null) {
                    mCallback.onVodLongClicked(vod);
                }
            }
        });
        mBinding.categoryRecyclerRecommend.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        mBinding.categoryRecyclerRecommend.setAdapter(mRecommendAdapter);
        setupRecommendKeyNavigation();

        // 3. Shelf 2: Sub-genre 1 (2:3 Portrait Shelf)
        mSubGenre1Adapter = new VodCardPortraitShelfAdapter(new VodCardPortraitShelfAdapter.OnVodClickListener() {
            @Override
            public void onVodFocused(Vod vod) {}

            @Override
            public void onVodClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodClicked(vod);
            }

            @Override
            public void onVodLongClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodLongClicked(vod);
            }
        });
        mBinding.categoryRecyclerSubGenre1.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        mBinding.categoryRecyclerSubGenre1.setAdapter(mSubGenre1Adapter);
        setupSubGenre1KeyNavigation();

        // 4. Shelf 3: Sub-genre 2 (2:3 Portrait Shelf)
        mSubGenre2Adapter = new VodCardPortraitShelfAdapter(new VodCardPortraitShelfAdapter.OnVodClickListener() {
            @Override
            public void onVodFocused(Vod vod) {}

            @Override
            public void onVodClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodClicked(vod);
            }

            @Override
            public void onVodLongClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodLongClicked(vod);
            }
        });
        mBinding.categoryRecyclerSubGenre2.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));
        mBinding.categoryRecyclerSubGenre2.setAdapter(mSubGenre2Adapter);
        setupSubGenre2KeyNavigation();

        // 5. Section 4: "全部影片" Filter Chips & 5-Column Grid
        mFilterAdapter = new FilterChipAdapter(this);
        mBinding.categoryFilterRecycler.setHorizontalSpacing(ResUtil.dp2px(10));
        mBinding.categoryFilterRecycler.setAdapter(mFilterAdapter);

        mGridAdapter = new VodCardPortraitAdapter(new VodCardPortraitAdapter.OnVodClickListener() {
            @Override
            public void onVodFocused(Vod vod) {}

            @Override
            public void onVodClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodClicked(vod);
            }

            @Override
            public void onVodLongClicked(Vod vod) {
                if (mCallback != null) mCallback.onVodLongClicked(vod);
            }
        });
        mBinding.categoryGrid.setLayoutManager(new GridLayoutManager(mActivity, 5));
        mBinding.categoryGrid.setAdapter(mGridAdapter);
        setupGridKeyNavigation();

        // 6. Scroll Listener for Parallax Top Bar Collapse & Hero Fade
        mBinding.categoryScrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if (mCallback != null) {
                mCallback.onCategoryScrolled(scrollY);
            }
            // Hero info subtle fade on deep scroll
            float fadeThreshold = ResUtil.dp2px(160);
            float alpha = Math.max(0f, 1.0f - (float) scrollY / fadeThreshold);
            mBinding.categoryHeroInfo.setAlpha(alpha);
        });
    }

    private void setupRecommendKeyNavigation() {
        mBinding.categoryRecyclerRecommend.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.categoryRecyclerRecommend.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) mRecommendFocusedPos = pos;

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        mBinding.categoryScrollView.smoothScrollTo(0, 0);
                        mBinding.categoryBtnPlay.requestFocus();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        jumpToNextShelfFromRecommend();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                        if (mCallback != null) mCallback.onOpenDrawer();
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

    private void setupSubGenre1KeyNavigation() {
        mBinding.categoryRecyclerSubGenre1.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.categoryRecyclerSubGenre1.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) mSubGenre1FocusedPos = pos;

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        jumpToRecommendFromSubGenre1();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        jumpToNextShelfFromSubGenre1();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                        if (mCallback != null) mCallback.onOpenDrawer();
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

    private void setupSubGenre2KeyNavigation() {
        mBinding.categoryRecyclerSubGenre2.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.categoryRecyclerSubGenre2.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) mSubGenre2FocusedPos = pos;

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        jumpToSubGenre1FromSubGenre2();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        jumpToAllSectionFromSubGenre2();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos == 0) {
                        if (mCallback != null) mCallback.onOpenDrawer();
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

    private void setupGridKeyNavigation() {
        mBinding.categoryGrid.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.categoryGrid.getChildAdapterPosition(v);
                    if (pos != RecyclerView.NO_POSITION) mGridFocusedPos = pos;

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (pos < 5) {
                            if (mBinding.categoryFilterRecycler.getVisibility() == View.VISIBLE && mBinding.categoryFilterRecycler.getChildCount() > 0) {
                                mBinding.categoryFilterRecycler.getChildAt(0).requestFocus();
                            } else {
                                jumpUpFromAllSection();
                            }
                            return true;
                        } else {
                            smoothScrollGridRow(-1);
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        smoothScrollGridRow(1);
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos % 5 == 0 && pos < 5) {
                        if (mCallback != null) mCallback.onOpenDrawer();
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

    private void jumpToRecommendFromHero() {
        int targetY = Math.max(0, mBinding.categoryHeaderRecommend.getTop() - ResUtil.dp2px(70));
        mBinding.categoryScrollView.smoothScrollTo(0, targetY);
        focusRecommendItem(mRecommendFocusedPos);
    }

    private void jumpToNextShelfFromRecommend() {
        if (mBinding.categoryHeaderSubGenre1.getVisibility() == View.VISIBLE && !mSubGenre1Adapter.isEmpty()) {
            int targetY = Math.max(0, mBinding.categoryHeaderSubGenre1.getTop() - ResUtil.dp2px(70));
            mBinding.categoryScrollView.smoothScrollTo(0, targetY);
            focusSubGenre1Item(mSubGenre1FocusedPos);
        } else {
            jumpToAllSectionFromSubGenre2();
        }
    }

    private void jumpToRecommendFromSubGenre1() {
        int targetY = Math.max(0, mBinding.categoryHeaderRecommend.getTop() - ResUtil.dp2px(70));
        mBinding.categoryScrollView.smoothScrollTo(0, targetY);
        focusRecommendItem(mRecommendFocusedPos);
    }

    private void jumpToNextShelfFromSubGenre1() {
        if (mBinding.categoryHeaderSubGenre2.getVisibility() == View.VISIBLE && !mSubGenre2Adapter.isEmpty()) {
            int targetY = Math.max(0, mBinding.categoryHeaderSubGenre2.getTop() - ResUtil.dp2px(70));
            mBinding.categoryScrollView.smoothScrollTo(0, targetY);
            focusSubGenre2Item(mSubGenre2FocusedPos);
        } else {
            jumpToAllSectionFromSubGenre2();
        }
    }

    private void jumpToSubGenre1FromSubGenre2() {
        if (mBinding.categoryHeaderSubGenre1.getVisibility() == View.VISIBLE && !mSubGenre1Adapter.isEmpty()) {
            int targetY = Math.max(0, mBinding.categoryHeaderSubGenre1.getTop() - ResUtil.dp2px(70));
            mBinding.categoryScrollView.smoothScrollTo(0, targetY);
            focusSubGenre1Item(mSubGenre1FocusedPos);
        } else {
            jumpToRecommendFromSubGenre1();
        }
    }

    private void jumpToAllSectionFromSubGenre2() {
        int targetY = Math.max(0, mBinding.categoryHeaderAll.getTop() - ResUtil.dp2px(70));
        mBinding.categoryScrollView.smoothScrollTo(0, targetY);
        if (mBinding.categoryFilterRecycler.getVisibility() == View.VISIBLE && mBinding.categoryFilterRecycler.getChildCount() > 0) {
            mBinding.categoryFilterRecycler.getChildAt(0).requestFocus();
        } else {
            focusGridItem(mGridFocusedPos % 5);
        }
    }

    private void jumpUpFromAllSection() {
        if (mBinding.categoryHeaderSubGenre2.getVisibility() == View.VISIBLE && !mSubGenre2Adapter.isEmpty()) {
            jumpToSubGenre1FromSubGenre2();
        } else if (mBinding.categoryHeaderSubGenre1.getVisibility() == View.VISIBLE && !mSubGenre1Adapter.isEmpty()) {
            jumpToRecommendFromSubGenre1();
        } else {
            mBinding.categoryScrollView.smoothScrollTo(0, 0);
            mBinding.categoryBtnPlay.requestFocus();
        }
    }

    private void smoothScrollGridRow(int direction) {
        App.post(() -> {
            View focused = mBinding.categoryGrid.getFocusedChild();
            if (focused != null) {
                int scrollY = mBinding.categoryHeaderAll.getTop() + focused.getTop() - ResUtil.dp2px(120);
                mBinding.categoryScrollView.smoothScrollTo(0, Math.max(0, scrollY));
            }
        }, 80);
    }

    public void focusHeroPlayButton() {
        mBinding.categoryScrollView.smoothScrollTo(0, 0);
        mBinding.categoryBtnPlay.requestFocus();
    }

    public void focusRecommendItem(int position) {
        mRecommendFocusedPos = position;
        App.post(() -> {
            mBinding.categoryRecyclerRecommend.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.categoryRecyclerRecommend.findViewHolderForAdapterPosition(position);
                if (vh != null) vh.itemView.requestFocus();
                else if (mBinding.categoryRecyclerRecommend.getChildCount() > 0) mBinding.categoryRecyclerRecommend.getChildAt(0).requestFocus();
            }, 50);
        }, 50);
    }

    public void focusSubGenre1Item(int position) {
        mSubGenre1FocusedPos = position;
        App.post(() -> {
            mBinding.categoryRecyclerSubGenre1.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.categoryRecyclerSubGenre1.findViewHolderForAdapterPosition(position);
                if (vh != null) vh.itemView.requestFocus();
                else if (mBinding.categoryRecyclerSubGenre1.getChildCount() > 0) mBinding.categoryRecyclerSubGenre1.getChildAt(0).requestFocus();
            }, 50);
        }, 50);
    }

    public void focusSubGenre2Item(int position) {
        mSubGenre2FocusedPos = position;
        App.post(() -> {
            mBinding.categoryRecyclerSubGenre2.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.categoryRecyclerSubGenre2.findViewHolderForAdapterPosition(position);
                if (vh != null) vh.itemView.requestFocus();
                else if (mBinding.categoryRecyclerSubGenre2.getChildCount() > 0) mBinding.categoryRecyclerSubGenre2.getChildAt(0).requestFocus();
            }, 50);
        }, 50);
    }

    public void focusGridItem(int position) {
        mGridFocusedPos = position;
        App.post(() -> {
            mBinding.categoryGrid.scrollToPosition(position);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mBinding.categoryGrid.findViewHolderForAdapterPosition(position);
                if (vh != null) vh.itemView.requestFocus();
                else if (mBinding.categoryGrid.getChildCount() > 0) mBinding.categoryGrid.getChildAt(0).requestFocus();
            }, 50);
        }, 50);
    }

    /**
     * Populates channel data: Hero carousel, Recommend shelf, Sub-genre shelves, All catalog grid & filters.
     */
    public void setCategoryData(Result result, Class categoryClass, String tabType) {
        if (result == null || result.getList() == null || result.getList().isEmpty()) {
            return;
        }

        List<Vod> all = result.getList();
        int total = all.size();

        // Configure "全部影片" Title based on active category
        String allTitle = "全部影片";
        if ("tv".equalsIgnoreCase(tabType) || (categoryClass != null && categoryClass.getTypeName() != null && categoryClass.getTypeName().contains("剧"))) {
            allTitle = "全部剧集";
        } else if ("variety".equalsIgnoreCase(tabType) || (categoryClass != null && categoryClass.getTypeName() != null && categoryClass.getTypeName().contains("综艺"))) {
            allTitle = "全部综艺";
        }
        mBinding.categoryHeaderAll.setText(allTitle);

        // 1. Hero Carousel: Top 4 items
        int heroCount = Math.min(total, 4);
        mHeroItems.clear();
        mHeroItems.addAll(all.subList(0, heroCount));
        mHeroIndex = 0;
        startHeroCarousel();

        // 2. Shelf 1: "推荐" (16:9 Landscape cards)
        int recommendStart = heroCount;
        int recommendEnd = Math.min(total, recommendStart + 8);
        List<Vod> recommendItems;
        if (recommendEnd > recommendStart) {
            recommendItems = new ArrayList<>(all.subList(recommendStart, recommendEnd));
        } else {
            recommendItems = new ArrayList<>(all.subList(0, Math.min(total, 6)));
        }
        mRecommendAdapter.setItems(recommendItems);

        // 3. Shelf 2 & 3: Sub-genre Shelves (2:3 Portrait cards)
        List<Filter> filters = categoryClass != null ? categoryClass.getFilters() : null;
        if ((filters == null || filters.isEmpty()) && result.getFilters() != null) {
            filters = result.getFilters().get(categoryClass != null ? categoryClass.getTypeId() : "");
        }

        String subGenre1Name = "热播精选";
        String subGenre2Name = "高分精选";
        if (filters != null && !filters.isEmpty() && filters.get(0).getValue() != null && filters.get(0).getValue().size() > 2) {
            List<Value> values = filters.get(0).getValue();
            // Pick 1st and 2nd valid sub-genres (skipping "全部" if present)
            int idx = 0;
            for (Value val : values) {
                if (!"全部".equals(val.getN()) && !TextUtils.isEmpty(val.getN())) {
                    if (idx == 0) subGenre1Name = val.getN() + "精选";
                    else if (idx == 1) subGenre2Name = val.getN() + "精选";
                    idx++;
                    if (idx >= 2) break;
                }
            }
        }

        if (total >= 16) {
            int shelf1Start = Math.min(total, recommendEnd);
            int shelf1End = Math.min(total, shelf1Start + 6);
            List<Vod> shelf1Items = new ArrayList<>(all.subList(shelf1Start, shelf1End));

            int shelf2Start = Math.min(total, shelf1End);
            int shelf2End = Math.min(total, shelf2Start + 6);
            List<Vod> shelf2Items = new ArrayList<>(all.subList(shelf2Start, shelf2End));

            if (!shelf1Items.isEmpty()) {
                mBinding.categoryHeaderSubGenre1.setText(subGenre1Name);
                mBinding.categoryHeaderSubGenre1.setVisibility(View.VISIBLE);
                mBinding.categoryRecyclerSubGenre1.setVisibility(View.VISIBLE);
                mSubGenre1Adapter.setItems(shelf1Items);
            } else {
                mBinding.categoryHeaderSubGenre1.setVisibility(View.GONE);
                mBinding.categoryRecyclerSubGenre1.setVisibility(View.GONE);
            }

            if (!shelf2Items.isEmpty()) {
                mBinding.categoryHeaderSubGenre2.setText(subGenre2Name);
                mBinding.categoryHeaderSubGenre2.setVisibility(View.VISIBLE);
                mBinding.categoryRecyclerSubGenre2.setVisibility(View.VISIBLE);
                mSubGenre2Adapter.setItems(shelf2Items);
            } else {
                mBinding.categoryHeaderSubGenre2.setVisibility(View.GONE);
                mBinding.categoryRecyclerSubGenre2.setVisibility(View.GONE);
            }
        } else {
            mBinding.categoryHeaderSubGenre1.setVisibility(View.GONE);
            mBinding.categoryRecyclerSubGenre1.setVisibility(View.GONE);
            mBinding.categoryHeaderSubGenre2.setVisibility(View.GONE);
            mBinding.categoryRecyclerSubGenre2.setVisibility(View.GONE);
        }

        // 4. Section 4: "全部影片" Filter Chips & 5-Column Grid
        if (filters != null && !filters.isEmpty() && filters.get(0).getValue() != null) {
            mFilterAdapter.setItems(filters.get(0).getValue());
            mBinding.categoryFilterRecycler.setVisibility(View.VISIBLE);
        } else {
            mBinding.categoryFilterRecycler.setVisibility(View.GONE);
        }
        mGridAdapter.setItems(all);
    }

    public void updateGridData(List<Vod> items) {
        if (items != null) {
            mGridAdapter.setItems(items);
        }
    }

    private void startHeroCarousel() {
        stopHeroCarousel();
        if (mHeroItems.isEmpty()) return;
        updateHeroDisplay(mHeroItems.get(mHeroIndex % mHeroItems.size()));

        if (mHeroItems.size() > 1) {
            mCarouselRunnable = () -> {
                if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
                mHeroIndex = (mHeroIndex + 1) % mHeroItems.size();
                updateHeroDisplay(mHeroItems.get(mHeroIndex));
                App.post(mCarouselRunnable, 7000);
            };
            App.post(mCarouselRunnable, 7000);
        }
    }

    private void stopHeroCarousel() {
        if (mCarouselRunnable != null) {
            App.removeCallbacks(mCarouselRunnable);
            mCarouselRunnable = null;
        }
    }

    private void updateHeroDisplay(Vod vod) {
        if (vod == null) return;
        mCurrentHeroVod = vod;

        mBinding.categoryHeroTitle.setText(vod.getName() != null ? vod.getName() : "");

        List<String> metas = new ArrayList<>();
        if (!TextUtils.isEmpty(vod.getRemarks())) metas.add(vod.getRemarks());
        if (!TextUtils.isEmpty(vod.getYear())) metas.add(vod.getYear());
        if (!TextUtils.isEmpty(vod.getArea())) metas.add(vod.getArea());
        if (!TextUtils.isEmpty(vod.getDirector())) metas.add("导演: " + vod.getDirector());
        mBinding.categoryHeroMeta.setText(TextUtils.join(" · ", metas));

        String desc = vod.getContent();
        if (TextUtils.isEmpty(desc)) desc = vod.getActor();
        mBinding.categoryHeroDesc.setText(desc != null ? desc : "");
        mBinding.categoryHeroDesc.setVisibility(TextUtils.isEmpty(desc) ? View.GONE : View.VISIBLE);

        if (!TextUtils.isEmpty(vod.getPic())) {
            Glide.with(mActivity)
                    .load(ImgUtil.getUrl(vod.getPic()))
                    .transition(DrawableTransitionOptions.withCrossFade(400))
                    .into(mBinding.categoryHeroBackdrop);
        }
    }

    @Override
    public void onFilterSelected(Value value) {
        if (mCallback != null) {
            mCallback.onFilterSelected(value);
        }
    }

    public void resetScroll() {
        mBinding.categoryScrollView.scrollTo(0, 0);
        mBinding.categoryHeroInfo.setAlpha(1.0f);
    }

    public void destroy() {
        stopHeroCarousel();
    }
}
