package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Value;

import java.util.ArrayList;
import java.util.List;

public class FilterChipAdapter extends RecyclerView.Adapter<FilterChipAdapter.ViewHolder> {

    private final List<Value> mItems = new ArrayList<>();
    private final OnClickListener mListener;
    private int mSelectedPosition = 0;

    public interface OnClickListener {
        void onFilterSelected(Value value);
    }

    public FilterChipAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public void setItems(List<Value> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        mSelectedPosition = 0;
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_filter_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Value item = mItems.get(position);
        holder.text.setText(item.getN());
        holder.text.setSelected(position == mSelectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int old = mSelectedPosition;
            mSelectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(mSelectedPosition);
            if (mListener != null) {
                mListener.onFilterSelected(item);
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
