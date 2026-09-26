package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class VodCardLandscapeAdapter extends RecyclerView.Adapter<VodCardLandscapeAdapter.ViewHolder> {

    private final List<Object> mItems = new ArrayList<>();
    private final OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemFocused(Object item);
        void onItemClicked(Object item);
        void onItemLongClicked(Object item);
    }

    public VodCardLandscapeAdapter(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void setItems(List<?> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_vod_card_landscape, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = mItems.get(position);
        if (item instanceof Vod vod) {
            holder.name.setText(vod.getName());
            holder.remark.setText(vod.getRemarks());
            holder.remark.setVisibility(TextUtils.isEmpty(vod.getRemarks()) ? View.GONE : View.VISIBLE);
            holder.progress.setVisibility(View.GONE);
            ImgUtil.load(vod.getName(), vod.getPic(), holder.image);
        } else if (item instanceof History history) {
            holder.name.setText(history.getVodName());
            holder.remark.setText(history.getVodRemarks());
            holder.remark.setVisibility(TextUtils.isEmpty(history.getVodRemarks()) ? View.GONE : View.VISIBLE);
            int progress = history.getDuration() > 0 ? (int) (history.getPosition() * 100 / history.getDuration()) : 0;
            holder.progress.setProgress(progress);
            holder.progress.setVisibility(progress > 0 ? View.VISIBLE : View.GONE);
            ImgUtil.load(history.getVodName(), history.getVodPic(), holder.image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClicked(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onItemLongClicked(item);
                return true;
            }
            return false;
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (holder.card != null) {
                holder.card.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).translationZ(hasFocus ? 6f : 0f).setDuration(160).start();
            }
            if (hasFocus && mListener != null) {
                mListener.onItemFocused(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final androidx.cardview.widget.CardView card;
        public final ImageView image;
        public final TextView name;
        public final TextView remark;
        public final ProgressBar progress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            remark = itemView.findViewById(R.id.remark);
            progress = itemView.findViewById(R.id.progress);
        }
    }
}
