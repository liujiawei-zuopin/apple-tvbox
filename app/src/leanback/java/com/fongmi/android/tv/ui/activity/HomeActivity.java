package com.fongmi.android.tv.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
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
import com.fongmi.android.tv.ui.adapter.TopNavAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardLandscapeAdapter;
import com.fongmi.android.tv.ui.adapter.VodCardPortraitAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.ImgUtil;
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

public class HomeActivity extends BaseActivity implements TopNavAdapter.OnTabListener, VodCardLandscapeAdapter.OnItemClickListener, VodCardPortraitAdapter.OnVodClickListener, FilterChipAdapter.OnClickListener, ConfigListener, SiteListener {

    private ActivityHomeBinding mBinding;
    private TopNavAdapter mTopNavAdapter;
    private VodCardLandscapeAdapter mWatchNowAdapter;
    private VodCardLandscapeAdapter mContinueAdapter;
    private VodCardPortraitAdapter mHotPicksAdapter;
    private FilterChipAdapter mFilterAdapter;
    private VodCardPortraitAdapter mCategoryGridAdapter;
    private SiteViewModel mViewModel;
    private Result mResult;
    private View mLastFocusedView;
    private int mCurrentTab = 0;
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

        setupTopNav();
        setupShelves();
        setupCategoryView();
        setupSideDrawer();
        setupViewModel();

        initConfig();
        setLogo();
    }

    @Override
    protected void initEvent() {
    }

    private void setupTopNav() {
        mBinding.btnMenuToggle.setOnClickListener(v -> openDrawer());
        mBinding.btnEmptyConfig.setOnClickListener(v -> ConfigDialog.create().vod().show(this));

        mTopNavAdapter = new TopNavAdapter(this);
        mBinding.topNavRecycler.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.topNavRecycler.setAdapter(mTopNavAdapter);
    }

    private void setupShelves() {
        mWatchNowAdapter = new VodCardLandscapeAdapter(this);
        mBinding.recyclerWatchNow.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.recyclerWatchNow.setAdapter(mWatchNowAdapter);

        mContinueAdapter = new VodCardLandscapeAdapter(this);
        mBinding.recyclerContinue.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.recyclerContinue.setAdapter(mContinueAdapter);

        mHotPicksAdapter = new VodCardPortraitAdapter(this);
        mBinding.gridHot.setLayoutManager(new GridLayoutManager(this, 5));
        mBinding.gridHot.setAdapter(mHotPicksAdapter);
    }

    private void setupCategoryView() {
        mFilterAdapter = new FilterChipAdapter(this);
        mBinding.categoryFilterRecycler.setHorizontalSpacing(ResUtil.dp2px(10));
        mBinding.categoryFilterRecycler.setAdapter(mFilterAdapter);

        mCategoryGridAdapter = new VodCardPortraitAdapter(this);
        mBinding.categoryGrid.setLayoutManager(new GridLayoutManager(this, 5));
        mBinding.categoryGrid.setAdapter(mCategoryGridAdapter);
    }

    private void setupSideDrawer() {
        mBinding.sideDrawer.menuSearch.setOnClickListener(v -> { closeDrawer(); SearchActivity.start(this); });
        mBinding.sideDrawer.menuHistory.setOnClickListener(v -> { closeDrawer(); CollectActivity.start(this, getString(R.string.home_history)); });
        mBinding.sideDrawer.menuLive.setOnClickListener(v -> { closeDrawer(); LiveActivity.start(this); });
        mBinding.sideDrawer.menuConfig.setOnClickListener(v -> { closeDrawer(); ConfigDialog.create().vod().show(this); });
        mBinding.sideDrawer.menuSite.setOnClickListener(v -> { closeDrawer(); SiteDialog.create().show(this); });
        mBinding.sideDrawer.menuCloud.setOnClickListener(v -> { closeDrawer(); startActivity(new Intent(this, FileActivity.class)); });
        mBinding.sideDrawer.menuCollect.setOnClickListener(v -> { closeDrawer(); KeepActivity.start(this); });
        mBinding.sideDrawer.menuPush.setOnClickListener(v -> { closeDrawer(); PushActivity.start(this); });
        mBinding.sideDrawer.menuSetting.setOnClickListener(v -> { closeDrawer(); SettingActivity.start(this); });
        mBinding.sideDrawer.drawerMask.setOnClickListener(v -> closeDrawer());

        // Key listeners for drawer items to close on Right DPAD
        View.OnKeyListener rightKeyListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                closeDrawer();
                return true;
            }
            return false;
        };
        mBinding.sideDrawer.menuSearch.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuHistory.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuLive.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuConfig.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuSite.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuCloud.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuCollect.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuPush.setOnKeyListener(rightKeyListener);
        mBinding.sideDrawer.menuSetting.setOnKeyListener(rightKeyListener);
    }

    private void setupViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            mBinding.progressLayout.showContent();
            if (mCurrentTab == 0) {
                populateHomeData(mResult = result);
                Cache.clear().put(result);
            } else {
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
        // Setup top nav tabs (Tab 0 is always Home)
        List<Class> tabs = new ArrayList<>();
        Class homeTab = new Class();
        homeTab.setTypeId("home");
        homeTab.setTypeName(getString(R.string.tab_home));
        tabs.add(homeTab);
        if (result != null && result.getTypes() != null) {
            tabs.addAll(result.getTypes());
        }
        mTopNavAdapter.setItems(tabs);

        // Populate shelves
        List<Vod> all = result != null && result.getList() != null ? result.getList() : new ArrayList<>();
        if (!all.isEmpty()) {
            mBinding.emptyView.setVisibility(View.GONE);
            mBinding.homeScrollView.setVisibility(View.VISIBLE);
            mBinding.heroInfoLayout.setVisibility(View.VISIBLE);

            int shelf1Size = Math.min(all.size(), 8);
            List<Vod> watchNow = all.subList(0, shelf1Size);
            List<Vod> hotPicks = all.subList(shelf1Size, all.size());
            mWatchNowAdapter.setItems(watchNow);
            mHotPicksAdapter.setItems(hotPicks);
            if (!watchNow.isEmpty()) {
                updateHero(watchNow.get(0));
            }

            // Focus first item if available
            App.post(() -> {
                if (mBinding.recyclerWatchNow.getChildCount() > 0) {
                    mBinding.recyclerWatchNow.getChildAt(0).requestFocus();
                } else {
                    mBinding.topNavRecycler.requestFocus();
                }
            }, 200);
        } else {
            mWatchNowAdapter.setItems(new ArrayList<>());
            mHotPicksAdapter.setItems(new ArrayList<>());
            mBinding.homeScrollView.setVisibility(View.GONE);
            mBinding.heroInfoLayout.setVisibility(View.GONE);
            mBinding.emptyView.setVisibility(View.VISIBLE);
            App.post(() -> mBinding.btnEmptyConfig.requestFocus(), 200);
        }
    }

    private void getHistory() {
        List<History> histories = History.get();
        if (histories != null && !histories.isEmpty()) {
            mBinding.headerContinue.setVisibility(View.VISIBLE);
            mBinding.recyclerContinue.setVisibility(View.VISIBLE);
            mContinueAdapter.setItems(histories);
        } else {
            mBinding.headerContinue.setVisibility(View.GONE);
            mBinding.recyclerContinue.setVisibility(View.GONE);
        }
    }

    private void populateCategoryData(Result result) {
        if (result == null) return;
        mCategoryGridAdapter.setItems(result.getList() != null ? result.getList() : new ArrayList<>());

        // Populate sub-category filters
        Class currentClass = mTopNavAdapter.getItem(mCurrentTab);
        List<Filter> filters = currentClass != null ? currentClass.getFilters() : null;
        if ((filters == null || filters.isEmpty()) && result.getFilters() != null) {
            filters = result.getFilters().get(currentClass != null ? currentClass.getTypeId() : "");
        }
        if (filters != null && !filters.isEmpty() && filters.get(0).getValue() != null) {
            mFilterAdapter.setItems(filters.get(0).getValue());
            mBinding.categoryFilterRecycler.setVisibility(View.VISIBLE);
        } else {
            mBinding.categoryFilterRecycler.setVisibility(View.GONE);
        }
    }

    private void updateHero(Vod vod) {
        if (vod == null) return;
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

        // Load background with 300ms smooth crossfade
        if (!isFinishing() && !isDestroyed() && !TextUtils.isEmpty(vod.getPic())) {
            Glide.with(this)
                    .load(ImgUtil.getUrl(vod.getPic()))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(mBinding.heroBackdrop);
        }
    }

    private void setLogo() {
        ImgUtil.logo(mBinding.logo);
    }

    @Override
    public void onTabFocused(int position, Class item) {
        switchTab(position, item);
    }

    @Override
    public void onTabClicked(int position, Class item) {
        switchTab(position, item);
    }

    private void switchTab(int position, Class item) {
        mCurrentTab = position;
        if (position == 0) {
            // Home View
            mBinding.homeScrollView.setVisibility(View.VISIBLE);
            mBinding.heroInfoLayout.setVisibility(View.VISIBLE);
            mBinding.categoryContainer.setVisibility(View.GONE);
            if (mResult != null) {
                populateHomeData(mResult);
            } else {
                mViewModel.homeContent();
            }
        } else {
            // Category View
            mBinding.homeScrollView.setVisibility(View.GONE);
            mBinding.heroInfoLayout.setVisibility(View.GONE);
            mBinding.categoryContainer.setVisibility(View.VISIBLE);
            mExtend.clear();
            mViewModel.categoryContent(getHome().getKey(), item.getTypeId(), "1", true, mExtend);
        }
    }

    @Override
    public void onFilterSelected(Value value) {
        Class currentClass = mTopNavAdapter.getItem(mCurrentTab);
        if (currentClass != null) {
            mExtend.put("class", value.getV());
            mViewModel.categoryContent(getHome().getKey(), currentClass.getTypeId(), "1", true, mExtend);
        }
    }

    @Override
    public void onItemFocused(Object item) {
        if (item instanceof Vod vod) {
            updateHero(vod);
        } else if (item instanceof History history) {
            Vod v = new Vod();
            v.setName(history.getVodName());
            v.setPic(history.getVodPic());
            v.setRemarks(history.getVodRemarks());
            updateHero(v);
        }
    }

    @Override
    public void onItemClicked(Object item) {
        if (item instanceof Vod vod) {
            onVodClicked(vod);
        } else if (item instanceof History history) {
            VideoActivity.start(this, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
        }
    }

    @Override
    public void onItemLongClicked(Object item) {
        if (item instanceof Vod vod) {
            onVodLongClicked(vod);
        } else if (item instanceof History history) {
            history.delete();
            getHistory();
        }
    }

    @Override
    public void onVodFocused(Vod vod) {
        updateHero(vod);
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

    public void openDrawer() {
        if (mBinding.sideDrawer.drawerLayout.getVisibility() == View.VISIBLE) return;
        mLastFocusedView = getCurrentFocus();
        mBinding.sideDrawer.drawerLayout.setVisibility(View.VISIBLE);
        mBinding.sideDrawer.drawerPanel.setTranslationX(-ResUtil.dp2px(320));
        mBinding.sideDrawer.drawerPanel.animate().translationX(0).setDuration(220).start();
        mBinding.sideDrawer.drawerMask.setAlpha(0f);
        mBinding.sideDrawer.drawerMask.animate().alpha(1f).setDuration(220).start();
        mBinding.sideDrawer.menuSearch.requestFocus();
    }

    public void closeDrawer() {
        if (mBinding.sideDrawer.drawerLayout.getVisibility() != View.VISIBLE) return;
        mBinding.sideDrawer.drawerPanel.animate().translationX(-ResUtil.dp2px(320)).setDuration(180).start();
        mBinding.sideDrawer.drawerMask.animate().alpha(0f).setDuration(180).withEndAction(() -> {
            mBinding.sideDrawer.drawerLayout.setVisibility(View.GONE);
            if (mLastFocusedView != null) {
                mLastFocusedView.requestFocus();
            }
        }).start();
    }

    private boolean isDrawerOpen() {
        return mBinding.sideDrawer.drawerLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else if (mCurrentTab != 0) {
            mTopNavAdapter.setSelectedPosition(0);
            switchTab(0, mTopNavAdapter.getItem(0));
            mBinding.topNavRecycler.requestFocus();
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
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
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
                setLogo();
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
