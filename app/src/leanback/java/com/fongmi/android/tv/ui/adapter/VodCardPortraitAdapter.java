package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class VodCardPortraitAdapter extends RecyclerView.Adapter<VodCardPortraitAdapter.ViewHolder> {

    private final List<Vod> mItems = new ArrayList<>();
    private final OnVodClickListener mListener;

    public interface OnVodClickListener {
        void onVodFocused(Vod vod);
        void onVodClicked(Vod vod);
        void onVodLongClicked(Vod vod);
    }

    public VodCardPortraitAdapter(OnVodClickListener listener) {
        this.mListener = listener;
    }

    public void setItems(List<Vod> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void addItems(List<Vod> items) {
        if (items == null || items.isEmpty()) return;
        int start = mItems.size();
        mItems.addAll(items);
        notifyItemRangeInserted(start, items.size());
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_vod_card_portrait, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = mItems.get(position);
        holder.name.setText(item.getVodName());
        holder.remark.setText(item.getVodRemarks());
        holder.remark.setVisibility(item.getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE);
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onVodClicked(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onVodLongClicked(item);
                return true;
            }
            return false;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && mListener != null) {
                mListener.onVodFocused(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView image;
        public final TextView name;
        public final TextView remark;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            remark = itemView.findViewById(R.id.remark);
        }
    }
}
