package com.aihealth.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aihealth.R;
import com.aihealth.data.entity.Drug;

import java.util.List;

/**
 * 药品列表适配器，支持普通编辑模式与批量选择模式。
 */
public class DrugAdapter extends RecyclerView.Adapter<DrugAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public interface OnEditClickListener {
        void onEditClick(int position);
    }

    public interface OnBatchSelectListener {
        void onBatchSelect(Drug drug, boolean isSelected);
    }

    private final List<Drug> drugList;
    private final List<Drug> selectedDrugs;
    private OnDeleteClickListener onDeleteClickListener;
    private OnEditClickListener onEditClickListener;
    private OnBatchSelectListener onBatchSelectListener;
    private boolean isBatchMode = false;

    public DrugAdapter(List<Drug> drugList, List<Drug> selectedDrugs) {
        this.drugList = drugList;
        this.selectedDrugs = selectedDrugs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drug, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Drug drug = drugList.get(position);

        holder.tvDrugName.setText(drug.getDrugName());

        List<String> timeList = drug.getTakeTimeListAsList();
        StringBuilder timeStr = new StringBuilder("服用时间: ");
        for (int i = 0; i < timeList.size(); i++) {
            if (i > 0) timeStr.append("、");
            timeStr.append(timeList.get(i));
        }
        holder.tvTakeTime.setText(timeStr.toString());

        holder.tvTakeTimes.setText("每日次数: " + drug.getTakeTimes() + "次");
        holder.tvTakeStatus.setText("状态: " + drug.getTakeStatus());
        holder.tvCreateTime.setText("创建时间: " + drug.getCreateTime());

        if ("已服用".equals(drug.getTakeStatus())) {
            holder.tvTakeStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
        } else {
            holder.tvTakeStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning));
        }

        if (isBatchMode) {
            holder.cbBatchSelect.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);

            holder.cbBatchSelect.setChecked(selectedDrugs.contains(drug));

            holder.cbBatchSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (onBatchSelectListener != null) {
                    onBatchSelectListener.onBatchSelect(drug, isChecked);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                boolean isChecked = !holder.cbBatchSelect.isChecked();
                holder.cbBatchSelect.setChecked(isChecked);
                if (onBatchSelectListener != null) {
                    onBatchSelectListener.onBatchSelect(drug, isChecked);
                }
            });
        } else {
            holder.cbBatchSelect.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(position);
                }
            });

            holder.btnEdit.setOnClickListener(v -> {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditClick(position);
                }
            });

            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return drugList.size();
    }

    public void setBatchMode(boolean batchMode) {
        isBatchMode = batchMode;
        notifyDataSetChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    public void setOnBatchSelectListener(OnBatchSelectListener listener) {
        this.onBatchSelectListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDrugName, tvTakeTime, tvTakeTimes, tvTakeStatus, tvCreateTime;
        Button btnEdit, btnDelete;
        CheckBox cbBatchSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDrugName = itemView.findViewById(R.id.tv_drug_name);
            tvTakeTime = itemView.findViewById(R.id.tv_take_time);
            tvTakeTimes = itemView.findViewById(R.id.tv_take_times);
            tvTakeStatus = itemView.findViewById(R.id.tv_take_status);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            cbBatchSelect = itemView.findViewById(R.id.cb_batch_select);
        }
    }
}
