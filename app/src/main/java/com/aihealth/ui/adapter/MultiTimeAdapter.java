package com.aihealth.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aihealth.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 多服用时间选择器的列表适配器。
 */
public class MultiTimeAdapter extends RecyclerView.Adapter<MultiTimeAdapter.TimeViewHolder> {

    public interface OnTimeClickListener {
        void onTimeClick(Calendar time, int position);
    }

    private final List<Calendar> timeList;
    private final OnTimeClickListener listener;

    public MultiTimeAdapter(List<Calendar> timeList, OnTimeClickListener listener) {
        this.timeList = timeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_picker, parent, false);
        return new TimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
        Calendar cal = timeList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText("时间" + (position + 1) + ": " + sdf.format(cal.getTime()));

        holder.tvTime.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeClick(cal, position);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeList.size();
    }

    public static class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;

        public TimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}
