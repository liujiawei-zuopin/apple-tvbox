package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.ui.adapter.TopNavAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Apple tvOS Centered Frosted Capsule Navigation Bar.
 */
public class TopNavController implements TopNavAdapter.OnTabListener {

    private final Activity mActivity;
    private final RecyclerView mRecycler;
    private final TopNavCallback mCallback;
    private final TopNavAdapter mAdapter;

    public interface TopNavCallback {
        void onTabSelected(int position, Class item);
        void onNavigateDown();
    }

    public TopNavController(Activity activity, RecyclerView recycler, TopNavCallback callback) {
        this.mActivity = activity;
        this.mRecycler = recycler;
        this.mCallback = callback;
        this.mAdapter = new TopNavAdapter(this);
        this.mRecycler.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        this.mRecycler.setAdapter(mAdapter);

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

    public void setTabs(List<Class> types) {
        List<Class> tabs = new ArrayList<>();
        Class homeTab = new Class();
        homeTab.setTypeId("home");
        homeTab.setTypeName(mActivity.getString(R.string.tab_home));
        tabs.add(homeTab);
        if (types != null) {
            tabs.addAll(types);
        }
        mAdapter.setItems(tabs);
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

    public int getItemCount() {
        return mAdapter.getItemCount();
    }

    public void requestFocus() {
        int sel = getSelectedPosition();
        if (sel >= 0 && sel < mAdapter.getItemCount()) {
            mRecycler.scrollToPosition(sel);
            RecyclerView.ViewHolder vh = mRecycler.findViewHolderForAdapterPosition(sel);
            if (vh != null) {
                vh.itemView.requestFocus();
                return;
            }
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
            mCallback.onTabSelected(position, item);
        }
    }
}
