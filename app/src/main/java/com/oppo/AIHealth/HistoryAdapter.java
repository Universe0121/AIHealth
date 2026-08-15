package com.oppo.AIHealth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyItems;
    private List<HistoryItem> filteredItems;
    private Context context;
    private OnItemClickListener listener;

    // 筛选类型
    private String currentFilter = "全部"; // 默认显示全部

    // 点击监听接口
    public interface OnItemClickListener {
        void onViewClick(int position, HistoryItem item);
        void onDeleteClick(int position, HistoryItem item);
    }

    public HistoryAdapter(Context context, List<HistoryItem> historyItems) {
        this.context = context;
        this.historyItems = historyItems;
        this.filteredItems = new ArrayList<>(historyItems);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = filteredItems.get(position);

        // 设置日期和时间
        holder.tvDate.setText(item.getDatePart());
        holder.tvTime.setText(item.getTimePart());

        // 设置诊断摘要
        holder.tvDiagnosisSummary.setText(item.getSummary());

        // 设置图片信息
        holder.tvImageInfo.setText(item.getFormattedImageInfo());

        // 设置疾病标签
        if (item.getDiseaseType() != null && !item.getDiseaseType().equals("其他")) {
            holder.tagDisease.setText(item.getDiseaseType());
            holder.tagDisease.setVisibility(View.VISIBLE);
        } else {
            holder.tagDisease.setVisibility(View.GONE);
        }

        // 设置紧急标签
        if (item.isUrgent()) {
            holder.tagUrgent.setVisibility(View.VISIBLE);
        } else {
            holder.tagUrgent.setVisibility(View.GONE);
        }

        // 设置点击事件
        holder.btnView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewClick(position, item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position, item);
            }
        });

        // 整个项点击事件（可选）
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    // 更新数据
    public void updateData(List<HistoryItem> newItems) {
        this.historyItems = newItems;
        applyFilter(currentFilter);
        notifyDataSetChanged();
    }

    // 添加新项目
    public void addItem(HistoryItem item) {
        historyItems.add(0, item); // 添加到开头
        applyFilter(currentFilter);
        notifyDataSetChanged();
    }

    // 删除项目
    public void removeItem(int position) {
        HistoryItem item = filteredItems.get(position);
        historyItems.remove(item);
        applyFilter(currentFilter);
        notifyDataSetChanged();
    }

    // 筛选功能
    public void filterByType(String filterType) {
        currentFilter = filterType;
        applyFilter(filterType);
        notifyDataSetChanged();
    }

    private void applyFilter(String filterType) {
        filteredItems.clear();

        if (filterType.equals("全部")) {
            filteredItems.addAll(historyItems);
        } else if (filterType.equals("今天")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());
            for (HistoryItem item : historyItems) {
                if (item.getDatePart().equals(today)) {
                    filteredItems.add(item);
                }
            }
        } else if (filterType.equals("最近7天")) {
            Date now = new Date();
            long sevenDaysAgo = now.getTime() - (7 * 24 * 60 * 60 * 1000);
            for (HistoryItem item : historyItems) {
                if (item.getTimestamp().getTime() >= sevenDaysAgo) {
                    filteredItems.add(item);
                }
            }
        } else if (filterType.equals("高血压")) {
            for (HistoryItem item : historyItems) {
                if (item.getDiseaseType().equals("高血压")) {
                    filteredItems.add(item);
                }
            }
        } else if (filterType.equals("糖尿病")) {
            for (HistoryItem item : historyItems) {
                if (item.getDiseaseType().equals("糖尿病")) {
                    filteredItems.add(item);
                }
            }
        } else if (filterType.equals("紧急")) {
            for (HistoryItem item : historyItems) {
                if (item.isUrgent()) {
                    filteredItems.add(item);
                }
            }
        }
    }

    // 获取筛选器选项
    public String[] getFilterOptions() {
        return new String[]{"全部", "今天", "最近7天", "高血压", "糖尿病", "紧急"};
    }

    // 获取当前筛选器
    public String getCurrentFilter() {
        return currentFilter;
    }

    // ViewHolder类
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvDiagnosisSummary, tvImageInfo;
        TextView tagDisease, tagUrgent;
        View btnView, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvDiagnosisSummary = itemView.findViewById(R.id.tv_diagnosis_summary);
            tvImageInfo = itemView.findViewById(R.id.tv_image_info);

            tagDisease = itemView.findViewById(R.id.tag_disease);
            tagUrgent = itemView.findViewById(R.id.tag_urgent);

            btnView = itemView.findViewById(R.id.btn_view);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}