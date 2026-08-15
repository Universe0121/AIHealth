package com.oppo.AIHealth.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.oppo.AIHealth.CameraActivity;
import com.oppo.AIHealth.R;
import com.oppo.AIHealth.activity.DrugCycleReceiver;
import com.oppo.AIHealth.data.AppDao;
import com.oppo.AIHealth.data.AppDatabaseA;
import com.oppo.AIHealth.data.Drug;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DrugManagementFragment extends Fragment {

    private static final String TAG = "DrugManagementFragment";
    private static final int REQUEST_CODE_DRUG_CAMERA = 1002;

    // 界面控件
    private EditText etDrugName, etTakeTimes;
    private RadioGroup rgTakeStatus;
    private Button btnSetTakeTime, btnSaveDrug, btnClear, btnDrugCamera, btnBatchEdit, btnBatchUsage, btnBatchReminder;
    private RecyclerView rvDrugList;
    private TextView tvEmptyDrugs;

    // 数据库相关
    private AppDatabaseA database;
    private AppDao appDao;

    // 数据相关
    private List<Drug> drugList;
    private DrugAdapter drugAdapter;
    private Calendar takeTime;
    private List<Calendar> takeTimeList = new ArrayList<>();

    // 批量编辑相关
    private boolean isBatchMode = false;
    private List<Drug> selectedDrugs = new ArrayList<>();
    private EditText etBatchUsageInput;

    // 定义在外部类的接口
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public interface OnEditClickListener {
        void onEditClick(int position);
    }

    public interface OnBatchSelectListener {
        void onBatchSelect(Drug drug, boolean isSelected);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drug, container, false);
        initViews(view);
        initDatabase();
        setupRecyclerView();
        setupButtonListeners();
        loadDrugs();
        return view;
    }

    private void initViews(View view) {
        etDrugName = view.findViewById(R.id.et_drug_name);
        etTakeTimes = view.findViewById(R.id.et_take_times);
        rgTakeStatus = view.findViewById(R.id.rg_take_status);
        btnSetTakeTime = view.findViewById(R.id.btn_set_take_time);
        btnSaveDrug = view.findViewById(R.id.btn_save_drug);
        btnClear = view.findViewById(R.id.btn_clear);
        rvDrugList = view.findViewById(R.id.rv_drug_list);
        tvEmptyDrugs = view.findViewById(R.id.tv_empty_drugs);

        btnDrugCamera = view.findViewById(R.id.btn_drug_camera);
        btnBatchEdit = view.findViewById(R.id.btn_batch_edit);
        btnBatchUsage = view.findViewById(R.id.btn_batch_usage);
        btnBatchReminder = view.findViewById(R.id.btn_batch_reminder);

        etBatchUsageInput = new EditText(requireContext());
        etBatchUsageInput.setHint("输入新的每日服用次数");
        etBatchUsageInput.setPadding(20, 20, 20, 20);

        takeTime = Calendar.getInstance();
        takeTime.set(Calendar.HOUR_OF_DAY, 8);
        takeTime.set(Calendar.MINUTE, 0);
        takeTime.set(Calendar.SECOND, 0);
        takeTimeList.add((Calendar) takeTime.clone());

        btnSetTakeTime.setText("设置服用时间: 08:00（点击添加多时间）");
        if (btnBatchUsage != null) btnBatchUsage.setVisibility(View.GONE);
        if (btnBatchReminder != null) btnBatchReminder.setVisibility(View.GONE);
    }

    private void initDatabase() {
        database = AppDatabaseA.getInstance(requireContext());
        appDao = database.appDao();
    }

    private void setupRecyclerView() {
        drugList = new ArrayList<>();
        rvDrugList.setLayoutManager(new LinearLayoutManager(requireContext()));
        drugAdapter = new DrugAdapter(drugList, selectedDrugs);
        rvDrugList.setAdapter(drugAdapter);

        drugAdapter.setOnDeleteClickListener(position -> showDeleteDialog(position));
        drugAdapter.setOnEditClickListener(position -> editDrug(position));
        drugAdapter.setOnBatchSelectListener((drug, isSelected) -> {
            if (isSelected) {
                selectedDrugs.add(drug);
            } else {
                selectedDrugs.remove(drug);
            }
            Log.d(TAG, "已选择: " + selectedDrugs.size() + " 个药品");
        });
    }

    private void setupButtonListeners() {
        btnSetTakeTime.setOnClickListener(v -> {
            String takeTimesStr = etTakeTimes.getText().toString().trim();
            int targetTimes = TextUtils.isEmpty(takeTimesStr) ? 1 : Integer.parseInt(takeTimesStr);
            showMultiTimePickerDialog(targetTimes);
        });

        btnSaveDrug.setOnClickListener(v -> saveDrug());
        btnClear.setOnClickListener(v -> clearForm());

        if (btnDrugCamera != null) {
            btnDrugCamera.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CameraActivity.class);
                intent.putExtra("source", "drug_management");
                startActivityForResult(intent, REQUEST_CODE_DRUG_CAMERA);
            });
        }

        if (btnBatchEdit != null) {
            btnBatchEdit.setOnClickListener(v -> {
                isBatchMode = !isBatchMode;
                drugAdapter.setBatchMode(isBatchMode);

                if (isBatchMode) {
                    selectedDrugs.clear();
                    btnBatchEdit.setText("取消批量编辑");
                    if (btnBatchUsage != null) btnBatchUsage.setVisibility(View.VISIBLE);
                    if (btnBatchReminder != null) btnBatchReminder.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "已进入批量编辑模式，请选择药品", Toast.LENGTH_SHORT).show();
                } else {
                    btnBatchEdit.setText("批量编辑");
                    if (btnBatchUsage != null) btnBatchUsage.setVisibility(View.GONE);
                    if (btnBatchReminder != null) btnBatchReminder.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "已退出批量编辑模式", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnBatchUsage != null) {
            btnBatchUsage.setOnClickListener(v -> {
                if (selectedDrugs.isEmpty()) {
                    Toast.makeText(requireContext(), "请先选择要修改的药品", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("批量修改服用次数")
                        .setMessage("为选中的 " + selectedDrugs.size() + " 个药品统一设置服用次数")
                        .setView(etBatchUsageInput)
                        .setPositiveButton("确认修改", (dialog, which) -> {
                            String timesStr = etBatchUsageInput.getText().toString().trim();
                            if (TextUtils.isEmpty(timesStr)) {
                                Toast.makeText(requireContext(), "请输入服用次数", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            int takeTimes;
                            try {
                                takeTimes = Integer.parseInt(timesStr);
                                if (takeTimes <= 0) {
                                    Toast.makeText(requireContext(), "请输入有效的服用次数", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(requireContext(), "请输入数字", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new Thread(() -> {
                                for (Drug drug : selectedDrugs) {
                                    drug.setTakeTimes(takeTimes);
                                    cancelDrugReminder(drug);
                                    // 使用正确的 update 方法
                                    appDao.updateDrug(drug);
                                    setDrugReminder(drug);
                                }

                                requireActivity().runOnUiThread(() -> {
                                    loadDrugs();
                                    etBatchUsageInput.setText("");
                                    Snackbar.make(requireView(),
                                                    "成功修改 " + selectedDrugs.size() + " 个药品的服用次数",
                                                    Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(getResources().getColor(R.color.success))
                                            .show();
                                });
                            }).start();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }

        if (btnBatchReminder != null) {
            btnBatchReminder.setOnClickListener(v -> {
                if (selectedDrugs.isEmpty()) {
                    Toast.makeText(requireContext(), "请先选择药品", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("批量管理用药提醒")
                        .setItems(new String[]{"开启所有选中药品提醒", "关闭所有选中药品提醒"},
                                (dialog, which) -> {
                                    new Thread(() -> {
                                        for (Drug drug : selectedDrugs) {
                                            if (which == 0) {
                                                setDrugReminder(drug);
                                            } else {
                                                cancelDrugReminder(drug);
                                            }
                                        }

                                        requireActivity().runOnUiThread(() -> {
                                            String msg = which == 0 ? "已开启提醒" : "已关闭提醒";
                                            Snackbar.make(requireView(),
                                                            msg + " (" + selectedDrugs.size() + "个药品)",
                                                            Snackbar.LENGTH_LONG)
                                                    .setBackgroundTint(getResources().getColor(R.color.success))
                                                    .show();
                                        });
                                    }).start();
                                })
                        .show();
            });
        }
    }

    private void showMultiTimePickerDialog(int targetTimes) {
        while (takeTimeList.size() < targetTimes) {
            Calendar defaultCal = Calendar.getInstance();
            defaultCal.set(Calendar.HOUR_OF_DAY, 8 + takeTimeList.size() * 4);
            defaultCal.set(Calendar.MINUTE, 0);
            takeTimeList.add(defaultCal);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View timeView = inflater.inflate(R.layout.dialog_multi_time_picker, null);
        RecyclerView rvTimeList = timeView.findViewById(R.id.rv_time_list);
        Button btnAddTime = timeView.findViewById(R.id.btn_add_time);
        Button btnRemoveTime = timeView.findViewById(R.id.btn_remove_time);

        MultiTimeAdapter timeAdapter = new MultiTimeAdapter(takeTimeList);
        rvTimeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTimeList.setAdapter(timeAdapter);

        btnAddTime.setOnClickListener(v -> {
            Calendar newCal = Calendar.getInstance();
            newCal.set(Calendar.HOUR_OF_DAY, 8 + takeTimeList.size() * 4);
            newCal.set(Calendar.MINUTE, 0);
            takeTimeList.add(newCal);
            timeAdapter.notifyItemInserted(takeTimeList.size() - 1);
        });

        btnRemoveTime.setOnClickListener(v -> {
            if (takeTimeList.size() > 1) {
                takeTimeList.remove(takeTimeList.size() - 1);
                timeAdapter.notifyItemRemoved(takeTimeList.size());
            } else {
                Toast.makeText(requireContext(), "至少保留一个服用时间", Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("设置多服用时间（共" + targetTimes + "次）")
                .setView(timeView)
                .setPositiveButton("确认", (dialog, which) -> {
                    StringBuilder timeStr = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    for (int i = 0; i < takeTimeList.size(); i++) {
                        if (i > 0) timeStr.append("、");
                        timeStr.append(sdf.format(takeTimeList.get(i).getTime()));
                    }
                    btnSetTakeTime.setText("服用时间: " + timeStr);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DRUG_CAMERA && resultCode == android.app.Activity.RESULT_OK && data != null) {
            int drugId = data.getIntExtra("drug_id", -1);
            if (drugId != -1) {
                loadDrugs();
                Snackbar.make(requireView(),
                                "药品识别并添加成功",
                                Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.success))
                        .show();
            } else {
                Toast.makeText(requireContext(), "药品添加失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTimePickerDialog(Calendar targetCal, int position) {
        int hour = targetCal.get(Calendar.HOUR_OF_DAY);
        int minute = targetCal.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute1) -> {
                    targetCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    targetCal.set(Calendar.MINUTE, minute1);
                    takeTimeList.set(position, targetCal);
                },
                hour, minute, true
        );

        timePickerDialog.setTitle("选择服用时间");
        timePickerDialog.show();
    }

    private void saveDrug() {
        String drugName = etDrugName.getText().toString().trim();
        String takeTimesStr = etTakeTimes.getText().toString().trim();

        if (TextUtils.isEmpty(drugName)) {
            etDrugName.setError("请输入药品名称");
            etDrugName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(takeTimesStr)) {
            etTakeTimes.setError("请输入服用次数");
            etTakeTimes.requestFocus();
            return;
        }

        int takeTimes;
        try {
            takeTimes = Integer.parseInt(takeTimesStr);
            if (takeTimes <= 0) {
                etTakeTimes.setError("请输入有效的服用次数");
                etTakeTimes.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etTakeTimes.setError("请输入有效的数字");
            etTakeTimes.requestFocus();
            return;
        }

        int selectedId = rgTakeStatus.getCheckedRadioButtonId();
        String takeStatus = "未服用";
        if (selectedId == R.id.rb_taken) {
            takeStatus = "已服用";
        }

        Drug drug = new Drug();
        drug.setDrugName(drugName);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String firstTime = timeFormat.format(takeTimeList.get(0).getTime());
        drug.setTakeTime(firstTime);
        List<String> timeStrList = new ArrayList<>();
        for (Calendar cal : takeTimeList) {
            timeStrList.add(timeFormat.format(cal.getTime()));
        }
        drug.setTakeTimeListFromList(timeStrList);

        drug.setTakeTimes(takeTimes);
        drug.setTakeStatus(takeStatus);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        drug.setCreateTime(dateFormat.format(new Date()));

        new Thread(() -> {
            long drugId = appDao.insertDrug(drug);
            drug.setId((int) drugId);

            requireActivity().runOnUiThread(() -> {
                Snackbar.make(requireView(),
                                "药品信息已保存",
                                Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.success))
                        .show();

                loadDrugs();
                clearForm();
                setDrugReminder(drug);
            });
        }).start();
    }

    private void setDrugReminder(Drug drug) {
        try {
            List<String> timeList = drug.getTakeTimeListAsList();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (int i = 0; i < timeList.size(); i++) {
                String timeStr = timeList.get(i);
                Date takeDate = timeFormat.parse(timeStr);

                if (takeDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(takeDate);

                    Calendar reminderTime = Calendar.getInstance();
                    reminderTime.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                    reminderTime.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                    reminderTime.set(Calendar.SECOND, 0);

                    if (reminderTime.getTimeInMillis() <= System.currentTimeMillis()) {
                        reminderTime.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager == null) {
                        Log.e(TAG, "AlarmManager is null, cannot set reminder");
                        return;
                    }

                    Intent intent = new Intent(requireContext(), DrugCycleReceiver.class);
                    intent.putExtra("drug_id", drug.getId());
                    intent.putExtra("drug_name", drug.getDrugName());
                    intent.putExtra("take_time", timeStr);

                    int requestCode = drug.getId() + i;
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            requireContext(),
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime.getTimeInMillis(),
                                pendingIntent
                        );
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime.getTimeInMillis(),
                                pendingIntent
                        );
                    }

                    Log.d(TAG, "药品提醒已设置: " + drug.getDrugName() + " 时间: " + timeStr);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "解析时间失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "设置提醒失败: " + e.getMessage());
        }
    }

    private void cancelDrugReminder(Drug drug) {
        try {
            List<String> timeList = drug.getTakeTimeListAsList();
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null, cannot cancel reminder");
                return;
            }

            for (int i = 0; i < timeList.size(); i++) {
                Intent intent = new Intent(requireContext(), DrugCycleReceiver.class);
                int requestCode = drug.getId() + i;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "已取消药品提醒: " + drug.getDrugName() + " 第" + (i+1) + "个时间");
            }
        } catch (Exception e) {
            Log.e(TAG, "取消提醒失败: " + e.getMessage());
        }
    }

    private void clearForm() {
        etDrugName.setText("");
        etTakeTimes.setText("");
        rgTakeStatus.check(R.id.rb_not_taken);
        takeTimeList.clear();
        takeTime.set(Calendar.HOUR_OF_DAY, 8);
        takeTime.set(Calendar.MINUTE, 0);
        takeTimeList.add((Calendar) takeTime.clone());
        btnSetTakeTime.setText("设置服用时间: 08:00（点击添加多时间）");
    }

    private void loadDrugs() {
        new Thread(() -> {
            List<Drug> drugs = appDao.getAllDrugs();
            drugList.clear();
            drugList.addAll(drugs);

            requireActivity().runOnUiThread(() -> {
                drugAdapter.notifyDataSetChanged();

                if (drugs.isEmpty()) {
                    tvEmptyDrugs.setVisibility(View.VISIBLE);
                    rvDrugList.setVisibility(View.GONE);
                } else {
                    tvEmptyDrugs.setVisibility(View.GONE);
                    rvDrugList.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void showDeleteDialog(int position) {
        Drug drug = drugList.get(position);

        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除药品 '" + drug.getDrugName() + "' 吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteDrug(position, drug))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteDrug(int position, Drug drug) {
        new Thread(() -> {
            // 先取消所有提醒
            cancelDrugReminder(drug);
            // 使用 DAO 方法删除数据库记录（修复点）
            appDao.deleteDrugById(drug.getId());

            requireActivity().runOnUiThread(() -> {
                drugList.remove(position);
                drugAdapter.notifyItemRemoved(position);

                Snackbar.make(requireView(),
                                "药品已删除",
                                Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.success))
                        .show();

                if (drugList.isEmpty()) {
                    tvEmptyDrugs.setVisibility(View.VISIBLE);
                    rvDrugList.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void editDrug(int position) {
        Drug drug = drugList.get(position);

        etDrugName.setText(drug.getDrugName());
        etTakeTimes.setText(String.valueOf(drug.getTakeTimes()));

        if ("已服用".equals(drug.getTakeStatus())) {
            rgTakeStatus.check(R.id.rb_taken);
        } else {
            rgTakeStatus.check(R.id.rb_not_taken);
        }

        List<String> timeList = drug.getTakeTimeListAsList();
        takeTimeList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        StringBuilder timeBtnStr = new StringBuilder("服用时间: ");
        for (int i = 0; i < timeList.size(); i++) {
            String timeStr = timeList.get(i);
            if (i > 0) timeBtnStr.append("、");
            timeBtnStr.append(timeStr);

            Calendar cal = Calendar.getInstance();
            try {
                cal.setTime(timeFormat.parse(timeStr));
                takeTimeList.add(cal);
            } catch (ParseException e) {
                Log.e(TAG, "解析时间失败: " + e.getMessage());
            }
        }
        btnSetTakeTime.setText(timeBtnStr);

        btnSaveDrug.setText("更新药品");
        final int drugPosition = position;
        btnSaveDrug.setOnClickListener(v -> updateDrug(drugPosition, drug));

        Toast.makeText(getContext(), "编辑药品信息", Toast.LENGTH_SHORT).show();
    }

    private void updateDrug(int position, Drug oldDrug) {
        String drugName = etDrugName.getText().toString().trim();
        String takeTimesStr = etTakeTimes.getText().toString().trim();

        if (TextUtils.isEmpty(drugName) || TextUtils.isEmpty(takeTimesStr)) {
            Toast.makeText(getContext(), "请填写所有必填项", Toast.LENGTH_SHORT).show();
            return;
        }

        int takeTimes;
        try {
            takeTimes = Integer.parseInt(takeTimesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的服用次数", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgTakeStatus.getCheckedRadioButtonId();
        String takeStatus = (selectedId == R.id.rb_taken) ? "已服用" : "未服用";

        oldDrug.setDrugName(drugName);
        oldDrug.setTakeTimes(takeTimes);
        oldDrug.setTakeStatus(takeStatus);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        oldDrug.setTakeTime(timeFormat.format(takeTimeList.get(0).getTime()));
        List<String> timeStrList = new ArrayList<>();
        for (Calendar cal : takeTimeList) {
            timeStrList.add(timeFormat.format(cal.getTime()));
        }
        oldDrug.setTakeTimeListFromList(timeStrList);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        oldDrug.setCreateTime(dateFormat.format(new Date()));

        new Thread(() -> {
            cancelDrugReminder(oldDrug);
            appDao.updateDrug(oldDrug);

            requireActivity().runOnUiThread(() -> {
                setDrugReminder(oldDrug);
                loadDrugs();
                btnSaveDrug.setText("保存药品");
                btnSaveDrug.setOnClickListener(v -> saveDrug());

                Snackbar.make(requireView(),
                                "药品信息已更新",
                                Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.success))
                        .show();

                clearForm();
            });
        }).start();
    }

    // 适配器内部类（保持不变，完整保留）
    private static class DrugAdapter extends RecyclerView.Adapter<DrugAdapter.ViewHolder> {
        private List<Drug> drugList;
        private List<Drug> selectedDrugs;
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

        class ViewHolder extends RecyclerView.ViewHolder {
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

    // 多时间选择适配器（保持不变）
    private class MultiTimeAdapter extends RecyclerView.Adapter<MultiTimeAdapter.TimeViewHolder> {
        private List<Calendar> timeList;

        public MultiTimeAdapter(List<Calendar> timeList) {
            this.timeList = timeList;
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
            holder.tvTime.setText("时间" + (position+1) + ": " + sdf.format(cal.getTime()));

            holder.tvTime.setOnClickListener(v -> {
                showTimePickerDialog(cal, position);
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return timeList.size();
        }

        class TimeViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime;

            public TimeViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_time);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDrugs();
    }
}