package com.oppo.AIHealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oppo.AIHealth.model.DietRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DietHistoryAdapter extends RecyclerView.Adapter<DietHistoryAdapter.ViewHolder> {

    private List<DietRecord> records;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DietRecord record);
    }

    public DietHistoryAdapter(List<DietRecord> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diet_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DietRecord record = records.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFood, tvCalories, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFood = itemView.findViewById(R.id.tv_food);
            tvCalories = itemView.findViewById(R.id.tv_calories);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(final DietRecord record, final OnItemClickListener listener) {
            tvFood.setText(record.getFoodItems());
            tvCalories.setText(String.format(Locale.US, "%.0f 千卡", record.getCalories()));
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(record.getTimestamp()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(record);
                }
            });
        }
    }
}