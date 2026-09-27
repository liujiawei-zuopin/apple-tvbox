package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.ui.adapter.TopNavAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Centered Frosted Capsule Navigation Bar.
 * Fixed 5 Tabs: "主页" (Home), "电影" (Movies), "剧集" (TV Series), "综艺" (Variety), "搜索" (Search 🔍).
 */
public class TopNavController implements TopNavAdapter.OnTabListener {

    public static final String ID_HOME = "home";
    public static final String ID_MOVIE = "movie";
    public static final String ID_TV = "tv";
    public static final String ID_VARIETY = "variety";
    public static final String ID_SEARCH = TopNavAdapter.TAB_SEARCH;

    private final Activity mActivity;
    private final RecyclerView mRecycler;
    private final TopNavCallback mCallback;
    private final TopNavAdapter mAdapter;

    public interface TopNavCallback {
        void onTabSelected(int position, Class item);
        void onTabClicked(int position, Class item);
        void onNavigateDown();
    }

    public TopNavController(Activity activity, RecyclerView recycler, TopNavCallback callback) {
        this.mActivity = activity;
        this.mRecycler = recycler;
        this.mCallback = callback;
        this.mAdapter = new TopNavAdapter(this);
        this.mRecycler.setItemAnimator(null);
        this.mRecycler.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        this.mRecycler.setAdapter(mAdapter);

        initFixedTabs();

        this.mRecycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (mCallback != null) {
                            mCallback.onNavigateDown();
                            return true;
                        }
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

    private void initFixedTabs() {
        List<Class> tabs = new ArrayList<>();
        
        Class homeTab = new Class();
        homeTab.setTypeId(ID_HOME);
        homeTab.setTypeName("主页");
        tabs.add(homeTab);

        Class movieTab = new Class();
        movieTab.setTypeId(ID_MOVIE);
        movieTab.setTypeName("电影");
        tabs.add(movieTab);

        Class tvTab = new Class();
        tvTab.setTypeId(ID_TV);
        tvTab.setTypeName("剧集");
        tabs.add(tvTab);

        Class varietyTab = new Class();
        varietyTab.setTypeId(ID_VARIETY);
        varietyTab.setTypeName("综艺");
        tabs.add(varietyTab);

        Class searchTab = new Class();
        searchTab.setTypeId(ID_SEARCH);
        searchTab.setTypeName("");
        tabs.add(searchTab);

        mAdapter.setItems(tabs);
    }

    public void setTabs(List<Class> types) {
        // Fixed standard tabs remain permanent
        initFixedTabs();
    }

    public int getSelectedPosition() {
        return mAdapter.getSelectedPosition();
    }

    public void setSelectedPosition(int position) {
        mAdapter.setSelectedPosition(position);
    }

    public Class getItem(int position) {
        return mAdapter.getItem(position);
    }

    public boolean hasFocus() {
        return mRecycler != null && mRecycler.hasFocus();
    }

    public void requestFocus() {
        int sel = getSelectedPosition();
        if (sel >= 0 && sel < mAdapter.getItemCount()) {
            mRecycler.scrollToPosition(sel);
            App.post(() -> {
                RecyclerView.ViewHolder vh = mRecycler.findViewHolderForAdapterPosition(sel);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else if (mRecycler.getChildCount() > 0) {
                    View child = mRecycler.getChildAt(Math.min(sel, mRecycler.getChildCount() - 1));
                    if (child != null) child.requestFocus();
                }
            });
            return;
        }
        mRecycler.requestFocus();
    }

    @Override
    public void onTabFocused(int position, Class item) {
        if (mCallback != null) {
            mCallback.onTabSelected(position, item);
        }
    }

    @Override
    public void onTabClicked(int position, Class item) {
        if (mCallback != null) {
            mCallback.onTabClicked(position, item);
        }
    }
}
