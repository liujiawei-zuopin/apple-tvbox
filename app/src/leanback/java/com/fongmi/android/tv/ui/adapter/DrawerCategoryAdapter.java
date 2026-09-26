package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;

import java.util.ArrayList;
import java.util.List;

public class DrawerCategoryAdapter extends RecyclerView.Adapter<DrawerCategoryAdapter.ViewHolder> {

    private final List<Class> mItems = new ArrayList<>();
    private final OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(Class item);
    }

    public DrawerCategoryAdapter(OnItemClickListener listener) {
        this.mListener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < mItems.size()) {
            Class item = mItems.get(position);
            return item.getTypeId() != null ? item.getTypeId().hashCode() : position;
        }
        return position;
    }

    public void setItems(List<Class> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_drawer_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = mItems.get(position);
        holder.text.setText(item.getTypeName());
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(item);
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
