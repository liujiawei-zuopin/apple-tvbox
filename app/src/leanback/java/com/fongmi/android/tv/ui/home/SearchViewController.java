package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Word;
import com.fongmi.android.tv.databinding.LayoutHomeSearchBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.PushActivity;
import com.fongmi.android.tv.ui.adapter.KeyboardAdapter;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Util;
import com.fongmi.android.tv.utils.ZhuToPin;
import com.github.catvod.net.OkHttp;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Controller for the Apple tvOS embedded Search page within HomeActivity.
 */
public class SearchViewController implements WordAdapter.OnClickListener, RecordAdapter.OnClickListener, KeyboardAdapter.OnClickListener {

    private final FragmentActivity mActivity;
    private final LayoutHomeSearchBinding mBinding;
    private final SearchCallback mCallback;

    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;
    private KeyboardAdapter mKeyboardAdapter;

    public interface SearchCallback {
        void onNavigateToTopNav();
        void onOpenDrawer();
    }

    public SearchViewController(FragmentActivity activity, LayoutHomeSearchBinding binding, SearchCallback callback) {
        this.mActivity = activity;
        this.mBinding = binding;
        this.mCallback = callback;
        initViews();
    }

    private void initViews() {
        // Keyboard Setup
        mBinding.keyboard.setItemAnimator(null);
        mBinding.keyboard.setHasFixedSize(false);
        mBinding.keyboard.addItemDecoration(new SpaceItemDecoration(7, 8));
        mBinding.keyboard.setAdapter(mKeyboardAdapter = new KeyboardAdapter(this));

        // Word & Record Recyclers
        mBinding.wordRecycler.setItemAnimator(null);
        mBinding.wordRecycler.setHasFixedSize(false);
        mBinding.wordRecycler.setLayoutManager(new FlexboxLayoutManager(mActivity, FlexDirection.ROW));
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));

        mBinding.recordRecycler.setHasFixedSize(false);
        mBinding.recordRecycler.setLayoutManager(new FlexboxLayoutManager(mActivity, FlexDirection.ROW));
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));

        // Event listeners
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onSearch();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                getWord(s.toString());
            }
        });
        mBinding.mic.setOnClickListener(v -> mBinding.mic.start());
        mBinding.mic.setListener(mActivity, new CustomTextListener() {
            @Override
            public void onResults(String result) {
                if (!result.isEmpty()) setKeyword(result);
                mBinding.keyword.requestFocus();
            }
        });

        setupKeyNavigation();
    }

    private void setupKeyNavigation() {
        mBinding.keyword.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (mCallback != null) {
                    mCallback.onNavigateToTopNav();
                    return true;
                }
            }
            return false;
        });

        mBinding.keyboard.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                view.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int pos = mBinding.keyboard.getChildAdapterPosition(v);
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && pos < 7) { // First row of keyboard
                        mBinding.keyword.requestFocus();
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

    public void onTabActivated() {
        mBinding.mic.setFocusable(true);
        if (empty()) {
            getHot();
        } else {
            getSuggest(mBinding.keyword.getText().toString().trim());
        }
        App.post(() -> mBinding.keyword.requestFocus(), 100);
    }

    private boolean empty() {
        return mBinding.keyword.getText().toString().trim().isEmpty();
    }

    public void setKeyword(String text) {
        mBinding.keyword.setText(text);
        mBinding.keyword.setSelection(text != null ? text.length() : 0);
    }

    private void getWord(String text) {
        if (text.isEmpty()) getHot();
        else getSuggest(text);
    }

    private void getHot() {
        mBinding.word.setText(R.string.search_hot);
        mWordAdapter.setItems(Word.objectFrom(Setting.getHot()).getData());
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1", Map.of(HttpHeaders.REFERER, "https://www.360kan.com/rank/general")).enqueue(getCallback(true));
    }

    private void getSuggest(String text) {
        mBinding.word.setText(R.string.search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + Uri.encode(ZhuToPin.get(text))).enqueue(getCallback(false));
    }

    private Callback getCallback(boolean hot) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = response.body().string();
                if (TextUtils.isEmpty(result)) return;
                App.post(() -> setAdapter(result, hot));
            }
        };
    }

    private void setAdapter(String result, boolean save) {
        if (!save && empty()) return;
        if (save) Setting.putHot(result);
        mWordAdapter.setItems(Word.objectFrom(result).getData());
    }

    @Override
    public void onItemClick(String text) {
        setKeyword(text);
        onSearch();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.recordLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }

    public void onSearch() {
        if (empty()) return;
        String keyword = mBinding.keyword.getText().toString().trim();
        App.post(() -> mRecordAdapter.add(keyword), 250);
        Util.hideKeyboard(mBinding.keyword);
        CollectActivity.start(mActivity, keyword);
    }

    // Keyboard callbacks
    @Override
    public void onTextClick(String text) {
        StringBuilder sb = new StringBuilder(mBinding.keyword.getText().toString());
        int cursor = mBinding.keyword.getSelectionStart();
        if (mBinding.keyword.length() > 19) return;
        sb.insert(cursor, text);
        mBinding.keyword.setText(sb.toString());
        mBinding.keyword.setSelection(cursor + 1);
    }

    @Override
    public void onIconClick(int resId) {
        StringBuilder sb = new StringBuilder(mBinding.keyword.getText().toString());
        int cursor = mBinding.keyword.getSelectionStart();
        if (resId == R.drawable.ic_setting_home) {
            SiteDialog.create().search().show(mActivity);
        } else if (resId == R.drawable.ic_keyboard_remote) {
            PushActivity.start(mActivity, 1);
        } else if (resId == R.drawable.ic_keyboard_search) {
            onSearch();
        } else if (resId == R.drawable.ic_keyboard_left) {
            mBinding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
        } else if (resId == R.drawable.ic_keyboard_right) {
            mBinding.keyword.setSelection(++cursor > mBinding.keyword.length() ? mBinding.keyword.length() : cursor);
        } else if (resId == R.drawable.ic_keyboard_back) {
            if (cursor > 0) {
                sb.deleteCharAt(cursor - 1);
                mBinding.keyword.setText(sb.toString());
                mBinding.keyword.setSelection(cursor - 1);
            }
        } else if (resId == R.drawable.ic_keyboard) {
            mKeyboardAdapter.toggle();
        }
    }

    @Override
    public boolean onLongClick(int resId) {
        if (resId != R.drawable.ic_keyboard_back) return false;
        mBinding.keyword.setText("");
        return true;
    }

    public void requestFocus() {
        mBinding.keyword.requestFocus();
    }
}
