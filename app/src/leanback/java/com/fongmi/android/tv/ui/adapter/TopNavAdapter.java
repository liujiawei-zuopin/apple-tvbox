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
    private RecyclerView mRecyclerView;
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
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView;
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
        if (position < 0 || position >= mItems.size()) return;
        if (mSelectedPosition == position) return;
        mSelectedPosition = position;
        // In-place selection state update without rebinding ViewHolders (prevents focus blink/flicker)
        if (mRecyclerView != null) {
            for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                View child = mRecyclerView.getChildAt(i);
                int pos = mRecyclerView.getChildAdapterPosition(child);
                if (pos != RecyclerView.NO_POSITION) {
                    child.setSelected(pos == mSelectedPosition);
                }
            }
        }
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
        } else {
            holder.icon.setVisibility(View.GONE);
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(item.getTypeName());
        }

        holder.itemView.setSelected(position == mSelectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (mSelectedPosition != pos) {
                setSelectedPosition(pos);
            }
            if (mListener != null) {
                mListener.onTabClicked(pos, item);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    if (mSelectedPosition != pos) {
                        setSelectedPosition(pos);
                    }
                }

                if (mDebounceRunnable != null) {
                    App.removeCallbacks(mDebounceRunnable);
                }
                mDebounceRunnable = () -> {
                    int p = holder.getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION && mListener != null) {
                        mListener.onTabFocused(p, item);
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
