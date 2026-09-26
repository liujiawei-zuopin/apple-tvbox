package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;

import java.util.ArrayList;
import java.util.List;

public class TopNavAdapter extends RecyclerView.Adapter<TopNavAdapter.ViewHolder> {

    public static final String TAB_SEARCH = "search";

    private final List<Class> mItems = new ArrayList<>();
    private final OnTabListener mListener;
    private int mSelectedPosition = 0;
    private Runnable mDebounceRunnable;

    public interface OnTabListener {
        void onTabFocused(int position, Class item);
        void onTabClicked(int position, Class item);
    }

    public TopNavAdapter(OnTabListener listener) {
        this.mListener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < mItems.size()) {
            Class c = mItems.get(position);
            return c.getTypeId() != null ? c.getTypeId().hashCode() : position;
        }
        return position;
    }

    public void setItems(List<Class> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    public Class getItem(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position) : null;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < mItems.size()) {
            Class item = mItems.get(position);
            if (TAB_SEARCH.equals(item.getTypeId())) {
                return; // Do not persist search as selected tab indicator
            }
        }
        if (mSelectedPosition == position) return;
        int old = mSelectedPosition;
        mSelectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(mSelectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_top_capsule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = mItems.get(position);
        boolean isSearch = TAB_SEARCH.equals(item.getTypeId());

        if (isSearch) {
            holder.text.setVisibility(View.GONE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.itemView.setSelected(false);
        } else {
            holder.icon.setVisibility(View.GONE);
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(item.getTypeName());
            holder.itemView.setSelected(position == mSelectedPosition);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (!isSearch && mSelectedPosition != pos) {
                setSelectedPosition(pos);
            }
            if (mListener != null) {
                mListener.onTabClicked(pos, item);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (mDebounceRunnable != null) {
                    App.removeCallbacks(mDebounceRunnable);
                }
                mDebounceRunnable = () -> {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && mListener != null) {
                        if (!isSearch && mSelectedPosition != pos) {
                            setSelectedPosition(pos);
                            mListener.onTabFocused(pos, item);
                        }
                    }
                };
                App.post(mDebounceRunnable, 180);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView text;
        public final ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}
