package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;

import java.util.ArrayList;
import java.util.List;

public class TopNavAdapter extends RecyclerView.Adapter<TopNavAdapter.ViewHolder> {

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
    }

    public void setItems(List<Class> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public Class getItem(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position) : null;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int position) {
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
        holder.text.setText(item.getTypeName());
        holder.text.setSelected(position == mSelectedPosition);

        holder.itemView.setOnClickListener(v -> {
            setSelectedPosition(holder.getBindingAdapterPosition());
            if (mListener != null) {
                mListener.onTabClicked(holder.getBindingAdapterPosition(), item);
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
                        setSelectedPosition(pos);
                        mListener.onTabFocused(pos, item);
                    }
                };
                App.post(mDebounceRunnable, 250);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView text;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
        }
    }
}
