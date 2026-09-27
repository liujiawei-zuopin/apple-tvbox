package com.fongmi.android.tv.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.BackupManager;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.FilterChipAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.home.CategoryViewController;
import com.fongmi.android.tv.ui.home.HeroViewController;
import com.fongmi.android.tv.ui.home.SearchViewController;
import com.fongmi.android.tv.ui.home.ShelfSectionController;
import com.fongmi.android.tv.ui.home.SideDrawerController;
import com.fongmi.android.tv.ui.home.TopNavController;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeActivity extends BaseActivity implements TopNavController.TopNavCallback, SideDrawerController.DrawerCallback, ShelfSectionController.ShelfCallback, VodCardPortraitAdapter.OnVodClickListener, FilterChipAdapter.OnClickListener, ConfigListener, SiteListener {

    private ActivityHomeBinding mBinding;
    private HeroViewController mHeroController;
    private TopNavController mTopNavController;
    private ShelfSectionController mShelfController;
    private SideDrawerController mDrawerController;
    private SearchViewController mSearchController;
    private CategoryViewController mCategoryController;

    private SiteViewModel mViewModel;
    private Result mResult;
    private List<Class> mRawTypes = new ArrayList<>();
    private int mCurrentTab = 0;
    private Class mCurrentCategoryClass;
    private final HashMap<String, String> mExtend = new HashMap<>();

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mResult = Result.empty();
        mBinding.progressLayout.showProgress();
        PermissionUtil.requestNotify(this);
        DLNARendererService.start(this);

        mHeroController = new HeroViewController(this, mBinding);
        mTopNavController = new TopNavController(this, mBinding.topNavRecycler, this);
        mShelfController = new ShelfSectionController(this, mBinding, this);
        mDrawerController = new SideDrawerController(this, mBinding.sideDrawer, this);
        mCategoryController = new CategoryViewController(this, mBinding.categoryContainer, new CategoryViewController.CategoryCallback() {
            @Override
            public void onVodClicked(Vod vod) {
                HomeActivity.this.onVodClicked(vod);
            }

            @Override
            public void onVodLongClicked(Vod vod) {
                HomeActivity.this.onVodLongClicked(vod);
            }

            @Override
            public void onFilterSelected(Value value) {
                HomeActivity.this.onFilterSelected(value);
            }

            @Override
            public void onNavigateToTopNav() {
                mBinding.topBar.setTranslationY(0);
                mBinding.topBar.setAlpha(1.0f);
                mTopNavController.requestFocus();
            }

            @Override
            public void onOpenDrawer() {
                openDrawer();
            }

            @Override
            public void onCategoryScrolled(int scrollY) {
                updateTopBarOnScroll(scrollY);
            }
        });
        mSearchController = new SearchViewController(this, mBinding.searchContainer, new SearchViewController.SearchCallback() {
            @Override
            public void onNavigateToTopNav() {
                mBinding.topBar.setTranslationY(0);
                mBinding.topBar.setAlpha(1.0f);
                mTopNavController.requestFocus();
            }

            @Override
            public void onOpenDrawer() {
                openDrawer();
            }
        });

        mBinding.btnEmptyConfig.setOnClickListener(v -> ConfigDialog.create().vod().show(this));

        // 1. Home ScrollView scrolling -> Parallax top bar collapse + Hero blur progression
        mBinding.homeScrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            mHeroController.onScroll(scrollY);
            updateTopBarOnScroll(scrollY);
        });

        // 2. Search Page ScrollView scrolling -> Parallax top bar collapse
        mBinding.searchContainer.scroll.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            updateTopBarOnScroll(scrollY);
        });

        setupViewModel();
        adjustHeroSpaceForSunkShelf();

        initConfig();
    }

    private void updateTopBarOnScroll(int scrollY) {
        if (mTopNavController != null && mTopNavController.hasFocus()) {
            mBinding.topBar.setTranslationY(0);
            mBinding.topBar.setAlpha(1.0f);
            return;
        }
        int maxCollapse = ResUtil.dp2px(50);
        mBinding.topBar.setTranslationY(-Math.min(scrollY, maxCollapse));
        float alpha = Math.max(0f, 1.0f - (float) scrollY / ResUtil.dp2px(85));
        mBinding.topBar.setAlpha(alpha);
    }

    @Override
    protected void initEvent() {
    }

    /**
     * Dynamically adjusts heroInfoLayout top margin according to screen viewport,
     * ensuring "Watch Now" shelf settles precisely at the bottom edge of the 1st screen.
     */
    private void adjustHeroSpaceForSunkShelf() {
        mBinding.contentContainer.post(() -> {
            int containerH = mBinding.contentContainer.getHeight();
            if (containerH <= 0) return;

            int topPadding = ResUtil.dp2px(48);
            int heroH = ResUtil.dp2px(115);
            int heroBottomMargin = ResUtil.dp2px(16);
            int shelf1H = ResUtil.dp2px(20) + ResUtil.dp2px(88); // Header + Recycler
            int bottomSafetyPadding = ResUtil.dp2px(20);

            int targetTopMargin = containerH - topPadding - heroH - heroBottomMargin - shelf1H - bottomSafetyPadding;
            if (targetTopMargin > ResUtil.dp2px(40)) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mBinding.heroInfoLayout.getLayoutParams();
                lp.topMargin = targetTopMargin;
                mBinding.heroInfoLayout.setLayoutParams(lp);
            }
        });
    }

    private void setupViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            mBinding.progressLayout.showContent();
            if (mCurrentTab == 0) {
                populateHomeData(mResult = result);
                Cache.clear().put(result);
            } else if (mCurrentTab != 4) {
                populateCategoryData(result);
            }
        });
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                mBinding.progressLayout.showContent();
                if (getHome() != null && !getHome().isEmpty()) {
                    mViewModel.homeContent();
                } else if (!VodConfig.get().getSites().isEmpty()) {
                    VodConfig.get().setHome(VodConfig.get().getSites().get(0));
                    mViewModel.homeContent();
                } else {
                    populateHomeData(Result.empty());
                }
                getHistory();
            }

            @Override
            public void error(String msg) {
                mBinding.progressLayout.showContent();
                if (!TextUtils.isEmpty(msg)) Notify.show(msg);
                if (getHome() != null && !getHome().isEmpty()) {
                    mViewModel.homeContent();
                } else {
                    populateHomeData(Result.empty());
                }
                getHistory();
            }
        };
    }

    @Override
    public void setConfig(Config config) {
        if (config == null) return;
        mBinding.progressLayout.showProgress();
        if (config.getUrl() != null && config.getUrl().startsWith("file")) {
            PermissionUtil.requestFile(this, allGranted -> loadConfig(config));
        } else {
            loadConfig(config);
        }
    }

    private void loadConfig(Config config) {
        switch (config.getType()) {
            case 0:
                VodConfig.load(config, getCallback());
                break;
            case 1:
                LiveConfig.load(config, getCallback());
                break;
            case 2:
                WallConfig.load(config, getCallback());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.history();
        RefreshEvent.home();
    }

    private void populateHomeData(Result result) {
        mRawTypes = result != null && result.getTypes() != null ? result.getTypes() : new ArrayList<>();
        mDrawerController.setCategories(mRawTypes);
        mTopNavController.setTabs(mRawTypes);

        boolean hasSites = !VodConfig.get().getSites().isEmpty();
        List<Vod> all = result != null && result.getList() != null ? result.getList() : new ArrayList<>();

        if (!all.isEmpty()) {
            mBinding.emptyView.setVisibility(View.GONE);
            mBinding.homeScrollView.setVisibility(View.VISIBLE);
            mHeroController.setVisibility(View.VISIBLE);

            int shelf1Size = Math.min(all.size(), 8);
            List<Vod> watchNow = all.subList(0, shelf1Size);
            List<Vod> hotPicks = all.subList(shelf1Size, all.size());
            mShelfController.setWatchNowData(watchNow);
            mShelfController.setHotPicksData(hotPicks);
            if (!watchNow.isEmpty()) {
                mHeroController.updateHero(watchNow.get(0));
            }

            App.post(() -> {
                if (getCurrentFocus() == null || getCurrentFocus() == mBinding.btnEmptyConfig) {
                    mShelfController.focusWatchNowItem(0);
                }
            }, 200);
        } else if (hasSites && !mRawTypes.isEmpty()) {
            mBinding.emptyView.setVisibility(View.GONE);
            mTopNavController.setSelectedPosition(1);
            switchTab(1, mTopNavController.getItem(1));
            App.post(() -> mTopNavController.requestFocus(), 200);
        } else if (hasSites) {
            mBinding.emptyView.setVisibility(View.GONE);
            mBinding.homeScrollView.setVisibility(View.VISIBLE);
            mHeroController.setVisibility(View.VISIBLE);
            mShelfController.clear();
            String title = getHome() != null && !TextUtils.isEmpty(getHome().getName()) ? getHome().getName() : "影视精选";
            mHeroController.setPlaceholder(title, "已连接 · 暂无首页推荐", "请按遥控器上方向键切换上方分类，或按 [菜单键] 切换站点线路");
            App.post(() -> mTopNavController.requestFocus(), 200);
        } else {
            mShelfController.clear();
            mBinding.homeScrollView.setVisibility(View.GONE);
            mHeroController.setVisibility(View.GONE);
            mBinding.emptyView.setVisibility(View.VISIBLE);
            App.post(() -> mBinding.btnEmptyConfig.requestFocus(), 200);
        }
    }

    private void getHistory() {
        List<History> histories = History.get();
        if (histories.isEmpty() && VodConfig.getUrl() != null && VodConfig.getUrl().contains("demo_config")) {
            History h1 = new History();
            h1.setKey("101");
            h1.setVodName("沙丘 2");
            h1.setVodPic("https://image.tmdb.org/t/p/w780/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg");
            h1.setVodRemarks("剩余 45 分钟");
            h1.setPosition(65);
            h1.setDuration(100);

            History h2 = new History();
            h2.setKey("102");
            h2.setVodName("奥本海默");
            h2.setVodPic("https://image.tmdb.org/t/p/w780/ptpr0kGAckfQkJeJIt8st5dglvd.jpg");
            h2.setVodRemarks("剩余 1 小时 20 分钟");
            h2.setPosition(40);
            h2.setDuration(100);

            List<History> demoHist = new ArrayList<>();
            demoHist.add(h1);
            demoHist.add(h2);
            mShelfController.setContinueData(demoHist);
            return;
        }
        mShelfController.setContinueData(histories);
    }

    private void populateCategoryData(Result result) {
        if (result == null) return;
        String tabType = "movie";
        if (mCurrentTab == 2) tabType = "tv";
        else if (mCurrentTab == 3) tabType = "variety";
        else if (mTopNavController.getItem(mCurrentTab) != null) {
            tabType = mTopNavController.getItem(mCurrentTab).getTypeId();
        }
        mCategoryController.setCategoryData(result, mCurrentCategoryClass, tabType);
    }

    @Override
    public void onTabSelected(int position, Class item) {
        if (item == null) return;
        switchTab(position, item);
    }

    @Override
    public void onTabClicked(int position, Class item) {
        if (item == null) return;
        switchTab(position, item);
    }

    @Override
    public void onNavigateDown() {
        if (mCurrentTab == 0) {
            mShelfController.focusWatchNowItem(0);
        } else if (mCurrentTab == 4 || TopNavController.ID_SEARCH.equals(mTopNavController.getItem(mCurrentTab) != null ? mTopNavController.getItem(mCurrentTab).getTypeId() : "")) {
            mSearchController.requestFocus();
        } else {
            mCategoryController.focusHeroPlayButton();
        }
    }

    @Override
    public void onNavigateToTopNav() {
        mBinding.topBar.setTranslationY(0);
        mBinding.topBar.setAlpha(1.0f);
        mTopNavController.requestFocus();
    }

    private Class findMappedCategory(String standardId) {
        if (mRawTypes == null || mRawTypes.isEmpty()) return null;
        String[] keywords;
        if (TopNavController.ID_MOVIE.equals(standardId)) {
            keywords = new String[]{"电影", "动作片", "喜剧片", "爱情片", "科幻片", "恐怖片", "剧情片", "战争片", "纪录片", "Movie", "片"};
        } else if (TopNavController.ID_TV.equals(standardId)) {
            keywords = new String[]{"电视剧", "连续剧", "国产剧", "美剧", "韩剧", "日剧", "港剧", "台剧", "海外剧", "剧集", "剧", "TV"};
        } else if (TopNavController.ID_VARIETY.equals(standardId)) {
            keywords = new String[]{"综艺", "真人秀", "娱乐", "Show"};
        } else {
            return null;
        }
        for (String kw : keywords) {
            for (Class c : mRawTypes) {
                if (c.getTypeName() != null && c.getTypeName().contains(kw)) {
                    return c;
                }
            }
        }
        return mRawTypes.get(0);
    }

    private void switchTab(int position, Class item) {
        mCurrentTab = position;
        if (position == 0 || TopNavController.ID_HOME.equals(item.getTypeId())) {
            // Home View with smooth transition
            mBinding.heroScrim.setVisibility(View.VISIBLE);
            mBinding.categoryAmbientBackdrop.setVisibility(View.GONE);
            mBinding.categoryAmbientTint.setVisibility(View.GONE);
            mBinding.categoryAmbientOverlay.setVisibility(View.GONE);
            mBinding.categoryContainer.getRoot().setVisibility(View.GONE);
            mBinding.searchContainer.getRoot().setVisibility(View.GONE);

            mBinding.homeScrollView.setAlpha(0f);
            mBinding.homeScrollView.setVisibility(View.VISIBLE);
            mBinding.homeScrollView.animate().alpha(1f).setDuration(200).start();
            mHeroController.setVisibility(View.VISIBLE);
            mHeroController.resetScroll();
            mBinding.topBar.setTranslationY(0);
            mBinding.topBar.setAlpha(1f);
            mBinding.homeScrollView.scrollTo(0, 0);

            if (mShelfController.isWatchNowEmpty()) {
                if (mResult != null) {
                    populateHomeData(mResult);
                } else {
                    mViewModel.homeContent();
                }
            }
        } else if (TopNavController.ID_SEARCH.equals(item.getTypeId())) {
            // Embedded Search Page with smooth transition
            mBinding.heroScrim.setVisibility(View.GONE);
            mBinding.homeScrollView.setVisibility(View.GONE);
            mBinding.categoryContainer.getRoot().setVisibility(View.GONE);
            mHeroController.setVisibility(View.GONE);

            mBinding.categoryAmbientBackdrop.setAlpha(0f);
            mBinding.categoryAmbientBackdrop.setVisibility(View.VISIBLE);
            mBinding.categoryAmbientBackdrop.animate().alpha(1f).setDuration(220).start();
            mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_default);
            mBinding.categoryAmbientTint.setVisibility(View.VISIBLE);
            mBinding.categoryAmbientOverlay.setVisibility(View.VISIBLE);

            mBinding.searchContainer.getRoot().setAlpha(0f);
            mBinding.searchContainer.getRoot().setTranslationY(ResUtil.dp2px(8));
            mBinding.searchContainer.getRoot().setVisibility(View.VISIBLE);
            mBinding.searchContainer.getRoot().animate().alpha(1f).translationY(0).setDuration(220).start();

            mBinding.topBar.setTranslationY(0);
            mBinding.topBar.setAlpha(1f);

            mSearchController.onTabActivated();
        } else {
            // Category View with smooth cross-fade transition and custom frosted ambient background
            Class mapped = findMappedCategory(item.getTypeId());
            mCurrentCategoryClass = mapped != null ? mapped : item;

            mBinding.heroScrim.setVisibility(View.GONE);
            mBinding.homeScrollView.setVisibility(View.GONE);
            mBinding.searchContainer.getRoot().setVisibility(View.GONE);
            mHeroController.setVisibility(View.GONE);

            // Set category ambient theme
            if (TopNavController.ID_MOVIE.equals(item.getTypeId())) {
                mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_movie_emerald);
            } else if (TopNavController.ID_TV.equals(item.getTypeId())) {
                mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_tv_sapphire);
            } else if (TopNavController.ID_VARIETY.equals(item.getTypeId())) {
                mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_variety_amethyst);
            } else {
                mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_default);
            }

            mBinding.categoryAmbientBackdrop.setAlpha(0f);
            mBinding.categoryAmbientBackdrop.setVisibility(View.VISIBLE);
            mBinding.categoryAmbientBackdrop.animate().alpha(1f).setDuration(220).start();

            mBinding.categoryAmbientTint.setAlpha(0f);
            mBinding.categoryAmbientTint.setVisibility(View.VISIBLE);
            mBinding.categoryAmbientTint.animate().alpha(1f).setDuration(220).start();

            mBinding.categoryAmbientOverlay.setVisibility(View.GONE);

            mBinding.categoryContainer.getRoot().setAlpha(0f);
            mBinding.categoryContainer.getRoot().setTranslationY(ResUtil.dp2px(8));
            mBinding.categoryContainer.getRoot().setVisibility(View.VISIBLE);
            mBinding.categoryContainer.getRoot().animate().alpha(1f).translationY(0).setDuration(220).start();

            mBinding.topBar.setTranslationY(0);
            mBinding.topBar.setAlpha(1f);

            mCategoryController.resetScroll();

            mExtend.clear();
            if (getHome() != null && mCurrentCategoryClass != null) {
                mViewModel.categoryContent(getHome().getKey(), mCurrentCategoryClass.getTypeId(), "1", true, mExtend);
            }
        }
    }

    // Side Drawer Raw Category Clicked
    @Override
    public void onCategorySelected(Class item) {
        if (item == null) return;
        mCurrentTab = -1;
        mCurrentCategoryClass = item;

        mBinding.homeScrollView.setVisibility(View.GONE);
        mBinding.searchContainer.getRoot().setVisibility(View.GONE);
        mHeroController.setVisibility(View.GONE);

        mBinding.categoryAmbientBackdrop.setAlpha(0f);
        mBinding.categoryAmbientBackdrop.setVisibility(View.VISIBLE);
        mBinding.categoryAmbientBackdrop.animate().alpha(1f).setDuration(220).start();

        mBinding.categoryAmbientTint.setBackgroundResource(R.drawable.bg_ambient_default);
        mBinding.categoryAmbientTint.setVisibility(View.VISIBLE);
        mBinding.categoryAmbientOverlay.setVisibility(View.GONE);

        mBinding.categoryContainer.getRoot().setAlpha(0f);
        mBinding.categoryContainer.getRoot().setTranslationY(ResUtil.dp2px(8));
        mBinding.categoryContainer.getRoot().setVisibility(View.VISIBLE);
        mBinding.categoryContainer.getRoot().animate().alpha(1f).translationY(0).setDuration(220).start();

        mBinding.topBar.setTranslationY(0);
        mBinding.topBar.setAlpha(1f);

        mCategoryController.resetScroll();

        mExtend.clear();
        if (getHome() != null) {
            mViewModel.categoryContent(getHome().getKey(), item.getTypeId(), "1", true, mExtend);
        }
        App.post(() -> mCategoryController.focusHeroPlayButton(), 300);
    }

    @Override
    public void onFilterSelected(Value value) {
        if (mCurrentCategoryClass != null && getHome() != null) {
            mExtend.put("class", value.getV());
            mViewModel.categoryContent(getHome().getKey(), mCurrentCategoryClass.getTypeId(), "1", true, mExtend);
        }
    }

    // Shelf & Poster Callbacks
    @Override
    public void onWatchNowFocused(Vod vod) {
        mHeroController.updateHero(vod);
    }

    @Override
    public void onVodFocused(Vod vod) {
        // Category grid item focused
    }

    @Override
    public void onVodClicked(Vod vod) {
        if (vod.isAction()) {
            mViewModel.action(getHome().getKey(), vod.getAction());
        } else if (getHome().isIndex()) {
            CollectActivity.start(this, vod.getName());
        } else {
            VideoActivity.start(this, getHome().getKey(), vod.getId(), vod.getName(), vod.getPic());
        }
    }

    @Override
    public void onVodLongClicked(Vod vod) {
        if (!vod.isAction()) {
            CollectActivity.start(this, vod.getName());
        }
    }

    @Override
    public void onHistoryClicked(History history) {
        VideoActivity.start(this, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
    }

    @Override
    public void onHistoryLongClicked(History history) {
        history.delete();
        getHistory();
    }

    @Override
    public void onOpenDrawer() {
        openDrawer();
    }

    public void openDrawer() {
        mDrawerController.openDrawer();
    }

    public void closeDrawer() {
        mDrawerController.closeDrawer();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
                mDrawerController.toggleDrawer();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (mDrawerController.isDrawerOpen()) {
            mDrawerController.closeDrawer();
        } else if (mCurrentTab != 0) {
            // Step-back: Switch back to Home Tab
            mTopNavController.setSelectedPosition(0);
            switchTab(0, mTopNavController.getItem(0));
            mTopNavController.requestFocus();
        } else if (mBinding.homeScrollView.getScrollY() > ResUtil.dp2px(20)) {
            // Step-back: Scroll back to top 1st screen and focus Watch Now
            mBinding.homeScrollView.smoothScrollTo(0, 0);
            mShelfController.focusWatchNowItem(0);
        } else {
            if (PlaybackService.isRunning()) {
                Util.moveToBackground(this);
            } else {
                super.onBackInvoked();
            }
        }
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getData()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            FileChooser.getUri(intent, uri -> loadLive(UrlUtil.toLocalUrl(uri)));
        } else {
            FileChooser.getUri(intent, uri -> VideoActivity.file(this, uri));
        }
    }

    private void loadLive(String url) {
        if (isFinishing() || isDestroyed()) return;
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.history();
                RefreshEvent.home();
                break;
            case COMMON:
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                mViewModel.homeContent();
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                mViewModel.homeContent();
                getHistory();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history());
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    protected void onDestroy() {
        if (mHeroController != null) {
            mHeroController.destroy();
        }
        if (mCategoryController != null) {
            mCategoryController.destroy();
        }
        DLNARendererService.stop(this);
        LiveConfig.get().clear();
        VodConfig.get().clear();
        BackupManager.backup();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
