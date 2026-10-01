package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
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
import com.fongmi.android.tv.utils.BlurUtil;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.ScrollCoordinator;
import com.fongmi.android.tv.utils.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple TV+ rich category channels ("电影", "剧集", "综艺"):
 * 1. Top Section: Hero Carousel Banner with centered synopsis, white play button, and indicator dots.
 * 2. Gradient Transition: Fades seamlessly from hero poster into the deep theme colored background.
 * 3. Shelf 1: "推荐" (16:9 Landscape cards, half-peeking at first screen bottom).
 * 4. Shelf 2+: Sub-genre Shelves (2:3 Portrait cards, 138dp x 214dp right-aligned with 16:9).
 * 5. Section 4: "全部影片" (Sub-category filters + 5-column poster grid).
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
        void onHeroBlurredReady(Bitmap blurred);
    }

    public CategoryViewController(Activity activity, LayoutCategoryChannelBinding binding, CategoryCallback callback) {
        this.mActivity = activity;
        this.mBinding = binding;
        this.mCallback = callback;
        initViews();
    }

    private void initViews() {
        int screenH = ResUtil.getScreenHeight();
        if (screenH > 0) {
            int peekingHeight = ResUtil.dp2px(64);
            int targetH = screenH - peekingHeight;
            android.view.ViewGroup.LayoutParams lp = mBinding.categoryHeroSection.getLayoutParams();
            if (lp != null) {
                lp.height = targetH;
                mBinding.categoryHeroSection.setLayoutParams(lp);
            }
        }

        // 1. Hero Play Button (Focus scale & D-Pad Carousel navigation)
        mBinding.categoryBtnPlay.setPivotX(0f);
        mBinding.categoryBtnPlay.setPivotY(ResUtil.dp2px(18));
        mBinding.categoryBtnPlay.setOnClickListener(v -> {
            if (mCurrentHeroVod != null && mCallback != null) {
                mCallback.onVodClicked(mCurrentHeroVod);
            }
        });
        mBinding.categoryBtnPlay.setOnFocusChangeListener((v, hasFocus) -> {
            v.setPivotX(0f);
            v.setPivotY(v.getHeight() > 0 ? v.getHeight() / 2f : ResUtil.dp2px(18));
            v.animate()
                    .scaleX(hasFocus ? 1.08f : 1.0f)
                    .scaleY(hasFocus ? 1.08f : 1.0f)
                    .setDuration(150)
                    .start();
            mBinding.categoryBtnPlayIcon.setColorFilter(hasFocus ? 0xFF121214 : 0xFFFFFFFF);
            mBinding.categoryBtnPlayText.setTextColor(hasFocus ? 0xFF121214 : 0xFFFFFFFF);
        });
        mBinding.categoryBtnPlay.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (mCallback != null) mCallback.onNavigateToTopNav();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                jumpToRecommendFromHero();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (mHeroItems.size() > 1) {
                    prevHeroCarousel();
                    return true;
                } else if (mCallback != null) {
                    mCallback.onOpenDrawer();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (mHeroItems.size() > 1) {
                    nextHeroCarousel();
                    return true;
                }
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

        // 3. Shelf 2: Sub-genre 1 (2:3 Portrait Shelf, 138dp x 214dp)
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

        // 4. Shelf 3: Sub-genre 2 (2:3 Portrait Shelf, 138dp x 214dp)
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
        setupFilterKeyNavigation();

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
            ScrollCoordinator.onScroll(scrollY, null, mBinding.categoryHeroInfo, mBinding.categoryHeroDots);
        });
    }

    private void setupIndicatorDots(int totalDots) {
        mBinding.categoryHeroDots.removeAllViews();
        if (totalDots <= 1) {
            mBinding.categoryHeroDots.setVisibility(View.GONE);
            return;
        }
        mBinding.categoryHeroDots.setVisibility(View.VISIBLE);
        int dotHeight = ResUtil.dp2px(6);
        int dotMargin = ResUtil.dp2px(5);

        for (int i = 0; i < totalDots; i++) {
            View dot = new View(mActivity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == 0 ? ResUtil.dp2px(16) : ResUtil.dp2px(6),
                    dotHeight
            );
            params.setMarginEnd(dotMargin);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0 ? R.drawable.dot_hero_carousel_active : R.drawable.dot_hero_carousel_inactive);
            mBinding.categoryHeroDots.addView(dot);
        }
    }

    private void updateIndicatorDots(int activeIndex) {
        int count = mBinding.categoryHeroDots.getChildCount();
        if (count == 0) return;
        int dotHeight = ResUtil.dp2px(6);
        int activeWidth = ResUtil.dp2px(16);
        int inactiveWidth = ResUtil.dp2px(6);

        for (int i = 0; i < count; i++) {
            View dot = mBinding.categoryHeroDots.getChildAt(i);
            boolean isActive = (i == activeIndex);
            int targetW = isActive ? activeWidth : inactiveWidth;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (params != null && params.width != targetW) {
                android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(params.width, targetW);
                anim.setDuration(200);
                anim.addUpdateListener(a -> {
                    params.width = (int) a.getAnimatedValue();
                    params.height = dotHeight;
                    dot.setLayoutParams(params);
                });
                anim.start();
            }
            dot.setBackgroundResource(isActive ? R.drawable.dot_hero_carousel_active : R.drawable.dot_hero_carousel_inactive);
        }
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

    private void setupFilterKeyNavigation() {
        mBinding.categoryFilterRecycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        jumpUpFromAllSection();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        focusGridItem(mGridFocusedPos % 5);
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
        int targetY = Math.max(0, mBinding.categoryHeaderRecommend.getTop() - ResUtil.dp2px(65));
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
        int targetY = Math.max(0, mBinding.categoryHeaderRecommend.getTop() - ResUtil.dp2px(65));
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
     * Category theme configuration hook.
     */
    public void updateCategoryTheme(String tabType) {
        // Hero poster is displayed clean and full-screen without obstructing masks.
    }

    /**
     * Populates channel data: Hero carousel, Recommend shelf, Sub-genre shelves, All catalog grid & filters.
     */
    public void setCategoryData(Result result, Class categoryClass, String tabType) {
        if (result == null || result.getList() == null || result.getList().isEmpty()) {
            return;
        }

        updateCategoryTheme(tabType);

        List<Vod> all = result.getList();
        int total = all.size();

        // Configure "全部XX" Title based on active category
        String typeName = categoryClass != null && !TextUtils.isEmpty(categoryClass.getTypeName()) ? categoryClass.getTypeName() : tabType;
        String allTitle = "全部影片";
        if (typeName != null) {
            String lower = typeName.toLowerCase();
            if (lower.contains("tv") || lower.contains("剧") || lower.contains("连续剧") || lower.contains("drama")) {
                allTitle = "全部剧集";
            } else if (lower.contains("variety") || lower.contains("综艺") || lower.contains("show")) {
                allTitle = "全部综艺";
            } else if (lower.contains("anime") || lower.contains("漫") || lower.contains("动画") || lower.contains("番剧")) {
                allTitle = "全部动漫";
            } else if (lower.contains("doc") || lower.contains("纪") || lower.contains("探索")) {
                allTitle = "全部纪录片";
            } else if (lower.contains("kid") || lower.contains("少儿") || lower.contains("儿童")) {
                allTitle = "全部少儿";
            } else if (lower.contains("sport") || lower.contains("体") || lower.contains("赛事")) {
                allTitle = "全部体育";
            } else if (lower.contains("short") || lower.contains("短剧")) {
                allTitle = "全部短剧";
            } else {
                allTitle = "全部" + typeName;
            }
        }
        mBinding.categoryHeaderAll.setText(allTitle);

        // 1. Hero Carousel: Top 4 items
        int heroCount = Math.min(total, 4);
        mHeroItems.clear();
        mHeroItems.addAll(all.subList(0, heroCount));
        mHeroIndex = 0;
        setupIndicatorDots(mHeroItems.size());
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
        String subGenre2Name = "高分推荐";
        if (typeName != null) {
            String lower = typeName.toLowerCase();
            if (lower.contains("movie") || lower.contains("电影") || lower.contains("片")) {
                subGenre1Name = "动作"; subGenre2Name = "爱情";
            } else if (lower.contains("tv") || lower.contains("剧")) {
                subGenre1Name = "国产剧"; subGenre2Name = "美剧";
            } else if (lower.contains("variety") || lower.contains("综艺")) {
                subGenre1Name = "真人秀"; subGenre2Name = "脱口秀";
            } else if (lower.contains("anime") || lower.contains("漫")) {
                subGenre1Name = "日本动漫"; subGenre2Name = "国产动漫";
            } else if (lower.contains("doc") || lower.contains("纪")) {
                subGenre1Name = "自然地理"; subGenre2Name = "人文历史";
            } else if (lower.contains("kid") || lower.contains("少儿")) {
                subGenre1Name = "益智启蒙"; subGenre2Name = "冒险动画";
            }
        }

        if (filters != null && !filters.isEmpty() && filters.get(0).getValue() != null) {
            List<Value> values = filters.get(0).getValue();
            List<String> validNames = new ArrayList<>();
            for (Value val : values) {
                if (!"全部".equals(val.getN()) && !TextUtils.isEmpty(val.getN())) {
                    validNames.add(val.getN());
                }
            }
            if (validNames.size() >= 1) subGenre1Name = validNames.get(0);
            if (validNames.size() >= 2) subGenre2Name = validNames.get(1);
        }

        // SubGenre 1 Shelf
        if (total >= 4) {
            List<Vod> shelf1Items = new ArrayList<>();
            int shelf1Count = Math.min(total, 6);
            for (int i = 0; i < shelf1Count; i++) {
                shelf1Items.add(all.get((i + 2) % total));
            }
            mBinding.categoryHeaderSubGenre1.setText(subGenre1Name);
            mBinding.categoryHeaderSubGenre1.setVisibility(View.VISIBLE);
            mBinding.categoryRecyclerSubGenre1.setVisibility(View.VISIBLE);
            mSubGenre1Adapter.setItems(shelf1Items);
        } else {
            mBinding.categoryHeaderSubGenre1.setVisibility(View.GONE);
            mBinding.categoryRecyclerSubGenre1.setVisibility(View.GONE);
        }

        // SubGenre 2 Shelf
        if (total >= 8) {
            List<Vod> shelf2Items = new ArrayList<>();
            int shelf2Count = Math.min(total, 6);
            for (int i = 0; i < shelf2Count; i++) {
                shelf2Items.add(all.get((i + 4) % total));
            }
            mBinding.categoryHeaderSubGenre2.setText(subGenre2Name);
            mBinding.categoryHeaderSubGenre2.setVisibility(View.VISIBLE);
            mBinding.categoryRecyclerSubGenre2.setVisibility(View.VISIBLE);
            mSubGenre2Adapter.setItems(shelf2Items);
        } else {
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

    public void prevHeroCarousel() {
        if (mHeroItems.isEmpty()) return;
        mHeroIndex = (mHeroIndex - 1 + mHeroItems.size()) % mHeroItems.size();
        animateHeroTransition(-1, mHeroItems.get(mHeroIndex));
        restartHeroCarouselTimer();
    }

    public void nextHeroCarousel() {
        if (mHeroItems.isEmpty()) return;
        mHeroIndex = (mHeroIndex + 1) % mHeroItems.size();
        animateHeroTransition(1, mHeroItems.get(mHeroIndex));
        restartHeroCarouselTimer();
    }

    private void restartHeroCarouselTimer() {
        stopHeroCarousel();
        if (mHeroItems.size() > 1) {
            mCarouselRunnable = () -> {
                if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
                mHeroIndex = (mHeroIndex + 1) % mHeroItems.size();
                animateHeroTransition(1, mHeroItems.get(mHeroIndex));
                App.post(mCarouselRunnable, 7000);
            };
            App.post(mCarouselRunnable, 7000);
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
                animateHeroTransition(1, mHeroItems.get(mHeroIndex));
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

    private void animateHeroTransition(int direction, Vod vod) {
        if (vod == null) return;
        mCurrentHeroVod = vod;

        // 1. Apple tvOS fluid text glide & cross-fade transition
        int slideOffset = ResUtil.dp2px(20) * direction;
        mBinding.categoryHeroTextGroup.animate().cancel();
        mBinding.categoryHeroTextGroup.animate()
                .alpha(0f)
                .translationX(-slideOffset)
                .setDuration(150)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(1.2f))
                .withEndAction(() -> {
                    mBinding.categoryHeroTitle.setText(vod.getName() != null ? vod.getName() : "");

                    List<String> metas = new ArrayList<>();
                    if (!TextUtils.isEmpty(vod.getRemarks())) metas.add(vod.getRemarks());
                    if (!TextUtils.isEmpty(vod.getYear())) metas.add(vod.getYear());
                    if (!TextUtils.isEmpty(vod.getArea())) metas.add(vod.getArea());
                    if (!TextUtils.isEmpty(vod.getDirector())) metas.add("导演: " + vod.getDirector());
                    mBinding.categoryHeroMeta.setText(TextUtils.join(" · ", metas));

                    String desc = vod.getContent();
                    if (TextUtils.isEmpty(desc)) desc = vod.getActor();
                    mBinding.categoryHeroDesc.setText(!TextUtils.isEmpty(desc) ? desc : "");

                    mBinding.categoryHeroTextGroup.setTranslationX(slideOffset);
                    mBinding.categoryHeroTextGroup.animate()
                            .alpha(1f)
                            .translationX(0)
                            .setDuration(260)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                            .start();
                })
                .start();

        // 2. Smoothly animate indicator dots
        updateIndicatorDots(mHeroIndex % Math.max(1, mHeroItems.size()));

        // 3. Smooth backdrop crossfade preserving previous drawable placeholder (zero flash)
        if (!TextUtils.isEmpty(vod.getPic())) {
            Object model = ImgUtil.getUrl(vod.getPic());
            if (mBinding.categoryHeroBackdrop != null) {
                Drawable currentDrawable = mBinding.categoryHeroBackdrop.getDrawable();
                com.bumptech.glide.RequestBuilder<Drawable> request = Glide.with(mActivity)
                        .load(model)
                        .transition(DrawableTransitionOptions.withCrossFade(350));
                if (currentDrawable != null) {
                    request = request.placeholder(currentDrawable);
                }
                request.into(mBinding.categoryHeroBackdrop);
            }
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
        mBinding.categoryHeroDesc.setText(!TextUtils.isEmpty(desc) ? desc : "");

        mBinding.categoryHeroTextGroup.setAlpha(1f);
        mBinding.categoryHeroTextGroup.setTranslationX(0);

        updateIndicatorDots(mHeroIndex % Math.max(1, mHeroItems.size()));

        if (!TextUtils.isEmpty(vod.getPic())) {
            Object model = ImgUtil.getUrl(vod.getPic());
            if (mBinding.categoryHeroBackdrop != null) {
                Glide.with(mActivity)
                        .load(model)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(mBinding.categoryHeroBackdrop);
            }
        }
    }

    public void adjustHeroLayout(int containerHeight) {
        if (containerHeight <= 0) return;
        int peekingHeight = ResUtil.dp2px(64);
        int targetHeroH = containerHeight - peekingHeight;
        if (targetHeroH > ResUtil.dp2px(300)) {
            android.view.ViewGroup.LayoutParams lp = mBinding.categoryHeroSection.getLayoutParams();
            if (lp != null && Math.abs(lp.height - targetHeroH) > ResUtil.dp2px(4)) {
                lp.height = targetHeroH;
                mBinding.categoryHeroSection.setLayoutParams(lp);
            }
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
        mBinding.categoryHeroDots.setAlpha(1.0f);
    }

    public void destroy() {
        stopHeroCarousel();
    }
}
