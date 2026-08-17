package com.aihealth.ui.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aihealth.receiver.DrugCycleReceiver;
import com.aihealth.R;

import java.util.ArrayList;
import java.util.List;

public class DrugCycleManagerActivity extends AppCompatActivity {
    private static final String TAG = "DrugCycleManager";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String CHANNEL_ID = "drug_cycle_reminder";

    // 控件
    private Button btnBackMain;
    private EditText etDrugName, etDosage, etCycleDays, etTakeTime;
    private Spinner spTakeFrequency;
    private Button btnSaveDrug, btnSetReminder, btnViewHistory;

    // 临时药品列表
    private List<DrugInfo> drugList = new ArrayList<>();
    private AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drug_cycle_manager);

        initView();
        initService();
        createNotificationChannel();
        checkPermissions();
        setClickEvents();

        Log.d(TAG, "药品管理页面初始化完成");
    }

    // 初始化控件（重点：完善所有控件的空指针检查）
    private void initView() {
        // 绑定返回按钮
        btnBackMain = findViewById(R.id.btn_back_main);
        // 绑定药品信息输入控件
        etDrugName = findViewById(R.id.et_drug_name);
        etDosage = findViewById(R.id.et_dosage);
        etCycleDays = findViewById(R.id.et_cycle_days);
        etTakeTime = findViewById(R.id.et_take_time);
        spTakeFrequency = findViewById(R.id.sp_take_frequency);
        // 绑定功能按钮
        btnSaveDrug = findViewById(R.id.btn_save_drug);
        btnSetReminder = findViewById(R.id.btn_set_reminder);
        btnViewHistory = findViewById(R.id.btn_view_history);

        // 逐个校验控件，避免空指针
        if (btnSaveDrug == null) {
            Log.e(TAG, "保存按钮未找到！ID: btn_save_drug");
            Toast.makeText(this, "⚠️ 保存按钮加载失败", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (etDrugName == null) {
            Log.e(TAG, "药品名称输入框未找到！ID: et_drug_name");
            Toast.makeText(this, "⚠️ 药品名称控件加载失败", Toast.LENGTH_LONG).show();
        }
        if (etDosage == null) {
            Log.e(TAG, "剂量输入框未找到！ID: et_dosage");
            Toast.makeText(this, "⚠️ 用药剂量控件加载失败", Toast.LENGTH_LONG).show();
        }
        if (spTakeFrequency == null) {
            Log.e(TAG, "服用频率选择器未找到！ID: sp_take_frequency");
            Toast.makeText(this, "⚠️ 服用频率控件加载失败", Toast.LENGTH_LONG).show();
        }
    }

    // 绑定所有点击事件（重点：强化保存按钮的反馈）
    private void setClickEvents() {
        // 返回主页面
        btnBackMain.setOnClickListener(v -> {
            Intent intent = new Intent(DrugCycleManagerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // 保存药品信息（核心修复：强化逻辑+反馈）
        btnSaveDrug.setOnClickListener(v -> {
            Log.d(TAG, "保存药品按钮被点击，开始执行保存逻辑");
            saveDrugInfo();
        });

        // 设置用药提醒
        btnSetReminder.setOnClickListener(v -> setDrugReminder());

        // 查看用药历史
        btnViewHistory.setOnClickListener(v -> {
            String historyTip = "📋 当前已保存 " + drugList.size() + " 种药品：\n";
            for (int i = 0; i < drugList.size(); i++) {
                historyTip += (i+1) + ". " + drugList.get(i).getName() + "（" + drugList.get(i).getDosage() + "）\n";
            }
            Toast.makeText(this, historyTip, Toast.LENGTH_LONG).show();
        });
    }

    // 保存药品信息（核心修复：防呆+强反馈）
    private void saveDrugInfo() {
        // 1. 获取输入内容（判空，避免空指针）
        String name = etDrugName != null ? etDrugName.getText().toString().trim() : "";
        String dosage = etDosage != null ? etDosage.getText().toString().trim() : "";
        String cycle = etCycleDays != null ? etCycleDays.getText().toString().trim() : "";
        String time = etTakeTime != null ? etTakeTime.getText().toString().trim() : "";
        String frequency = spTakeFrequency != null ? spTakeFrequency.getSelectedItem().toString() : "每天1次";

        Log.d(TAG, "获取到的药品信息：名称=" + name + "，剂量=" + dosage);

        // 2. 严格输入校验（强化提示，定位问题）
        if (name.isEmpty()) {
            Toast.makeText(this, "❌ 请输入药品名称（如：感冒药）", Toast.LENGTH_LONG).show();
            etDrugName.requestFocus(); // 光标定位到名称输入框，更友好
            return;
        }
        if (dosage.isEmpty()) {
            Toast.makeText(this, "❌ 请输入用药剂量（如：每天1次，每次1片）", Toast.LENGTH_LONG).show();
            etDosage.requestFocus(); // 光标定位到剂量输入框
            return;
        }

        // 3. 保存到临时列表
        DrugInfo drug = new DrugInfo(name, dosage, frequency, cycle, time);
        drugList.add(drug);

        // 4. 强反馈（多维度确认保存成功）
        // ① Toast提示（带具体药品名，更直观）
        Toast.makeText(this, "✅ 保存成功！\n药品：" + name + "\n剂量：" + dosage, Toast.LENGTH_LONG).show();
        // ② 清空输入框（视觉反馈，提示用户可输入下一个）
        if (etDrugName != null) etDrugName.setText("");
        if (etDosage != null) etDosage.setText("");
        if (etCycleDays != null) etCycleDays.setText("");
        if (etTakeTime != null) etTakeTime.setText("");
        // ③ 日志记录
        Log.d(TAG, "药品保存成功，当前列表总数：" + drugList.size());
    }

    // 以下方法无修改，保留原有逻辑
    private void initService() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Toast.makeText(this, "闹钟服务初始化失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "药品周期提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("药品全周期管理的用药提醒");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void checkPermissions() {
        List<String> needPerms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needPerms.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SCHEDULE_EXACT_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
            needPerms.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM);
        }

        if (!needPerms.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    needPerms.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void setDrugReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予通知权限", Toast.LENGTH_SHORT).show();
            return;
        }

        if (drugList.isEmpty()) {
            Toast.makeText(this, "请先保存药品信息", Toast.LENGTH_SHORT).show();
            return;
        }

        DrugInfo lastDrug = drugList.get(drugList.size() - 1);
        Intent intent = new Intent(this, DrugCycleReceiver.class);
        intent.putExtra("msg", "用药提醒：" + lastDrug.getName() + "，" + lastDrug.getDosage());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, flags
        );

        long triggerTime = SystemClock.elapsedRealtime() + 10000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
        }

        Toast.makeText(this, "用药提醒已设置（10秒后触发）", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "部分权限未授予，可能影响提醒功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class DrugInfo {
        private String name;
        private String dosage;
        private String frequency;
        private String cycleDays;
        private String takeTime;

        public DrugInfo(String name, String dosage, String frequency, String cycleDays, String takeTime) {
            this.name = name;
            this.dosage = dosage;
            this.frequency = frequency;
            this.cycleDays = cycleDays;
            this.takeTime = takeTime;
        }

        public String getName() { return name; }
        public String getDosage() { return dosage; }
    }
}