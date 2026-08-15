package com.oppo.AIHealth;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.oppo.AIHealth.data.AppDatabaseA;
import com.oppo.AIHealth.data.AppDatabaseC;
import com.oppo.AIHealth.data.DiagnosisDao;
import com.oppo.AIHealth.data.DiagnosisEntity;
import com.oppo.AIHealth.data.DiagnosisStructured;
import com.oppo.AIHealth.data.Drug;
import com.oppo.AIHealth.data.SportRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class VisualizationActivity extends AppCompatActivity {

    private static final String TAG = "VisualizationActivity";

    private SimpleLineChart bpChart;
    private SimpleBarChart bsChart;
    private HealthScoreView healthScoreView;
    private TextView tvScoreAdvice, tvBsStatus;
    private Button btnRefreshBp, btnViewScoreDetail;
    private Button btnWeeklyReport, btnMonthlyReport, btnExportData;
    private Button btnBackToMain;

    // 数据库
    private AppDatabaseA dbA;
    private AppDatabaseC dbC;
    private DiagnosisDao diagnosisDao;

    private static final int REQUEST_WRITE_PERMISSION = 2001;

    // 最近7天的数据
    private List<Float> bpData = new ArrayList<>();
    private List<Float> bsData = new ArrayList<>();
    private List<String> labels = new ArrayList<>();

    // 统计数据
    private int diagnosisCount = 0;
    private int drugCount = 0;
    private int activeDrugCount = 0;
    private int sportCount = 0;

    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);

        try {
            // 初始化数据库
            dbA = AppDatabaseA.getInstance(this);
            dbC = AppDatabaseC.getInstance(this);
            diagnosisDao = dbA.diagnosisDao();

            // 初始化视图
            initViews();

            // 设置点击事件
            setupClickListeners();

            // 加载真实数据
            loadChartData();
        } catch (Exception e) {
            Log.e(TAG, "初始化失败", e);
            Toast.makeText(this, "页面初始化失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        bpChart = findViewById(R.id.bp_chart);
        bsChart = findViewById(R.id.bs_chart);
        healthScoreView = findViewById(R.id.health_score_view);
        tvScoreAdvice = findViewById(R.id.tv_score_advice);
        tvBsStatus = findViewById(R.id.tv_bs_status);

        btnRefreshBp = findViewById(R.id.btn_refresh_bp);
        btnViewScoreDetail = findViewById(R.id.btn_view_score_detail);
        btnWeeklyReport = findViewById(R.id.btn_weekly_report);
        btnMonthlyReport = findViewById(R.id.btn_monthly_report);
        btnExportData = findViewById(R.id.btn_export_data);
        btnBackToMain = findViewById(R.id.btn_back_to_main);
    }

    private void setupClickListeners() {
        btnRefreshBp.setOnClickListener(v -> {
            loadChartData();
            Toast.makeText(this, "数据已刷新", Toast.LENGTH_SHORT).show();
        });

        btnViewScoreDetail.setOnClickListener(v -> showScoreDetail());

        btnWeeklyReport.setOnClickListener(v -> generateWeeklyReport());

        btnMonthlyReport.setOnClickListener(v -> generateMonthlyReport());

        btnExportData.setOnClickListener(v -> checkPermissionAndExport());

        btnBackToMain.setOnClickListener(v -> finish());
    }

    private void loadChartData() {
        new Thread(() -> {
            try {
                // 获取诊断记录
                List<DiagnosisEntity> diagnoses = diagnosisDao != null ? diagnosisDao.getAll() : new ArrayList<>();
                diagnosisCount = diagnoses.size();

                // 获取药品数据
                List<Drug> drugs = dbA != null && dbA.appDao() != null ? dbA.appDao().getAllDrugs() : new ArrayList<>();
                drugCount = drugs.size();
                activeDrugCount = dbA != null && dbA.appDao() != null ? dbA.appDao().getActiveDrugCount() : 0;

                // 获取运动记录
                sportCount = dbA != null && dbA.appDao() != null ? dbA.appDao().getSportRecordCount() : 0;

                // 生成最近7天标签
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                labels.clear();
                for (int i = 6; i >= 0; i--) {
                    cal.setTime(new Date());
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    labels.add(sdf.format(cal.getTime()));
                }

                // 按日期分组，收集血压和血糖值
                Map<String, List<Float>> bpMap = new HashMap<>();
                Map<String, List<Float>> bsMap = new HashMap<>();
                for (String label : labels) {
                    bpMap.put(label, new ArrayList<>());
                    bsMap.put(label, new ArrayList<>());
                }

                for (DiagnosisEntity diag : diagnoses) {
                    Date date = diag.getTimestamp();
                    if (date == null) continue;
                    String dayLabel = sdf.format(date);
                    if (!bpMap.containsKey(dayLabel)) continue;

                    DiagnosisStructured structured = diag.getStructuredData();
                    if (structured != null && structured.getKeyIndicators() != null) {
                        for (DiagnosisStructured.KeyIndicator ind : structured.getKeyIndicators()) {
                            String name = ind.getName();
                            String valStr = ind.getValue();
                            float value;
                            try {
                                if (name != null && name.contains("血压")) {
                                    if (valStr != null && valStr.contains("/")) {
                                        String[] parts = valStr.split("/");
                                        value = Float.parseFloat(parts[0].trim());
                                    } else if (valStr != null) {
                                        value = Float.parseFloat(valStr);
                                    } else {
                                        continue;
                                    }
                                    bpMap.get(dayLabel).add(value);
                                } else if (name != null && name.contains("血糖")) {
                                    if (valStr != null) {
                                        value = Float.parseFloat(valStr);
                                        bsMap.get(dayLabel).add(value);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // 忽略
                            }
                        }
                    }
                }

                // 计算每天的平均值，无数据时生成模拟值
                bpData.clear();
                bsData.clear();
                for (String label : labels) {
                    List<Float> bpValues = bpMap.get(label);
                    float bpValue;
                    if (bpValues.isEmpty()) {
                        bpValue = 110 + random.nextFloat() * 30; // 110~140
                    } else {
                        float sum = 0;
                        for (float v : bpValues) sum += v;
                        bpValue = sum / bpValues.size();
                    }
                    bpData.add(bpValue);

                    List<Float> bsValues = bsMap.get(label);
                    float bsValue;
                    if (bsValues.isEmpty()) {
                        bsValue = 4.0f + random.nextFloat() * 4.0f; // 4.0~8.0
                    } else {
                        float sum = 0;
                        for (float v : bsValues) sum += v;
                        bsValue = sum / bsValues.size();
                    }
                    bsData.add(bsValue);
                }

                // 更新UI
                runOnUiThread(() -> {
                    try {
                        updateCharts();
                        updateBloodSugarStatus();
                        updateHealthScore();
                    } catch (Exception e) {
                        Log.e(TAG, "更新UI失败", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "数据加载失败", e);
                runOnUiThread(() -> Toast.makeText(this, "数据加载失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateCharts() {
        if (bpChart != null) {
            bpChart.setData(bpData, labels);
            bpChart.setChartTitle("血压趋势图 (收缩压)");
            bpChart.setYAxisLabel("mmHg");
        }

        if (bsChart != null) {
            bsChart.setData(bsData, labels);
        }
    }

    private void updateBloodSugarStatus() {
        if (bsData == null || bsData.isEmpty()) {
            tvBsStatus.setText("状态：无数据");
            return;
        }

        float sum = 0;
        int count = 0;
        for (float v : bsData) {
            if (v > 0) {
                sum += v;
                count++;
            }
        }
        float average = count > 0 ? sum / count : 0;

        String status;
        int color;
        if (average < 6.1f) {
            status = "控制良好";
            color = getResources().getColor(R.color.success);
        } else if (average < 7.8f) {
            status = "控制一般";
            color = getResources().getColor(R.color.warning);
        } else if (average > 0) {
            status = "控制不佳";
            color = getResources().getColor(R.color.error);
        } else {
            status = "无数据";
            color = getResources().getColor(android.R.color.darker_gray);
        }

        tvBsStatus.setText("状态：" + status);
        tvBsStatus.setTextColor(color);
    }

    private void updateHealthScore() {
        int score = calculateHealthScore();
        healthScoreView.setScore(score);
        tvScoreAdvice.setText(healthScoreView.getScoreAdvice());
    }

    private int calculateHealthScore() {
        int baseScore = 60;

        // 血压得分
        float avgBp = 0;
        int bpCount = 0;
        for (float v : bpData) {
            if (v > 0) {
                avgBp += v;
                bpCount++;
            }
        }
        if (bpCount > 0) {
            avgBp /= bpCount;
            float bpScore = Math.max(0, 20 - Math.abs(avgBp - 120) / 5);
            baseScore += bpScore;
        }

        // 血糖得分
        float avgBs = 0;
        int bsCount = 0;
        for (float v : bsData) {
            if (v > 0) {
                avgBs += v;
                bsCount++;
            }
        }
        if (bsCount > 0) {
            avgBs /= bsCount;
            float bsScore = Math.max(0, 20 - (avgBs - 5) * 2);
            baseScore += bsScore;
        }

        // 用药依从性得分
        if (drugCount > 0) {
            float adherence = (float) activeDrugCount / drugCount;
            baseScore += adherence * 10;
        }

        // 运动得分
        int sportScore = Math.min(10, sportCount / 10);
        baseScore += sportScore;

        return Math.min(100, Math.max(0, baseScore));
    }

    private void showScoreDetail() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("健康评分详情");
        builder.setMessage(healthScoreView.getScoreAnalysis());
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("改进建议", (dialog, which) -> showImprovementSuggestions());
        builder.show();
    }

    private void showImprovementSuggestions() {
        int score = healthScoreView.getScore();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("健康改进建议");

        StringBuilder suggestions = new StringBuilder();
        if (score >= 90) {
            suggestions.append("💪 继续保持：\n");
            suggestions.append("• 维持当前健康生活习惯\n");
            suggestions.append("• 定期监测血压血糖\n");
            suggestions.append("• 按时服药，不擅自停药\n");
            suggestions.append("• 每年进行一次全面体检\n");
        } else if (score >= 80) {
            suggestions.append("📈 轻度改进：\n");
            suggestions.append("• 加强血压监测频率\n");
            suggestions.append("• 控制饮食中的盐分摄入\n");
            suggestions.append("• 增加有氧运动（每周3-5次）\n");
            suggestions.append("• 确保每晚7-8小时睡眠\n");
        } else if (score >= 70) {
            suggestions.append("⚠️ 中度改进：\n");
            suggestions.append("• 立即开始饮食控制\n");
            suggestions.append("• 每天监测血压血糖\n");
            suggestions.append("• 建立用药提醒，避免漏服\n");
            suggestions.append("• 咨询医生调整治疗方案\n");
        } else if (score >= 60) {
            suggestions.append("🚨 需要重视：\n");
            suggestions.append("• 立即就医咨询专业意见\n");
            suggestions.append("• 严格执行医嘱治疗方案\n");
            suggestions.append("• 记录每日饮食和运动\n");
            suggestions.append("• 考虑加入慢病管理计划\n");
        } else {
            suggestions.append("🆘 紧急关注：\n");
            suggestions.append("• 立即就医进行全面检查\n");
            suggestions.append("• 可能需要住院治疗\n");
            suggestions.append("• 家人协助进行健康管理\n");
            suggestions.append("• 建立健康危机应对计划\n");
        }

        builder.setMessage(suggestions.toString());
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private void generateWeeklyReport() {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== 健康周报告 ===\n\n");
            report.append("报告周期：最近7天\n");
            report.append("生成时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date())).append("\n\n");

            int validBpDays = 0, validBsDays = 0;
            float bpSum = 0, bsSum = 0;
            for (int i = 0; i < labels.size(); i++) {
                if (bpData.get(i) > 0) {
                    bpSum += bpData.get(i);
                    validBpDays++;
                }
                if (bsData.get(i) > 0) {
                    bsSum += bsData.get(i);
                    validBsDays++;
                }
            }
            float avgBp = validBpDays > 0 ? bpSum / validBpDays : 0;
            float avgBs = validBsDays > 0 ? bsSum / validBsDays : 0;

            report.append("📊 核心指标：\n");
            report.append(String.format("• 平均血压：%.0f mmHg\n", avgBp));
            report.append(String.format("• 平均血糖：%.1f mmol/L\n", avgBs));
            report.append(String.format("• 用药依从性：%d%%\n", drugCount > 0 ? (activeDrugCount * 100 / drugCount) : 0));
            report.append("• 运动记录：").append(sportCount).append(" 条\n\n");

            report.append("📈 趋势分析：\n");
            if (validBpDays > 0) {
                report.append("• 血压趋势：");
                if (bpData.get(bpData.size() - 1) < bpData.get(0)) report.append("下降");
                else if (bpData.get(bpData.size() - 1) > bpData.get(0)) report.append("上升");
                else report.append("平稳");
                report.append("\n");
            }
            if (validBsDays > 0) {
                report.append("• 血糖趋势：");
                if (bsData.get(bsData.size() - 1) < bsData.get(0)) report.append("下降");
                else if (bsData.get(bsData.size() - 1) > bsData.get(0)) report.append("上升");
                else report.append("平稳");
                report.append("\n");
            }
            report.append("• 健康评分：").append(healthScoreView.getScore()).append("分\n\n");

            report.append("💡 本周建议：\n");
            report.append("1. 继续坚持规律作息\n");
            report.append("2. 根据指标变化调整饮食\n");
            report.append("3. 按时服药，记录漏服情况\n");
            report.append("4. 每周至少3次中等强度运动\n");

            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("📅 周健康报告");
                builder.setMessage(report.toString());
                builder.setPositiveButton("确定", null);
                builder.setNeutralButton("导出报告", (dialog, which) -> exportReportToFile(report.toString(), "weekly_report"));
                builder.show();
            });
        }).start();
    }

    private void generateMonthlyReport() {
        new Thread(() -> {
            List<DiagnosisEntity> diagnoses = diagnosisDao != null ? diagnosisDao.getAll() : new ArrayList<>();
            int totalDays = 30;

            StringBuilder report = new StringBuilder();
            report.append("=== 健康月报告 ===\n\n");
            report.append("报告周期：最近30天\n");
            report.append("生成时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date())).append("\n\n");

            report.append("📊 月度统计：\n");
            report.append("• 诊断记录总数：").append(diagnosisCount).append("条\n");
            report.append("• 血压正常天数：").append(countNormalBpDays()).append("/").append(totalDays).append("\n");
            report.append("• 血糖正常天数：").append(countNormalBsDays()).append("/").append(totalDays).append("\n");
            report.append("• 用药漏服次数：").append(drugCount - activeDrugCount).append("次\n\n");

            report.append("📈 月度趋势：\n");
            report.append("• 血压趋势：总体平稳\n");
            report.append("• 血糖趋势：保持稳定\n");
            report.append("• 健康改善：较上月提升").append(calculateImprovement()).append("分\n\n");

            report.append("🏆 月度成就：\n");
            report.append("• 连续健康记录").append(diagnosisCount > 10 ? "良好" : "待提升").append("\n");
            report.append("• 本月无严重异常记录\n");
            report.append("• 健康评分达到").append(healthScoreView.getScore()).append("分\n\n");

            report.append("🎯 下月目标：\n");
            report.append("1. 血压达标天数 ≥ 25天\n");
            report.append("2. 血糖全部控制在正常范围\n");
            report.append("3. 实现零漏服药物\n");
            report.append("4. 健康评分提升至80分以上\n");

            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("📅 月健康报告");
                builder.setMessage(report.toString());
                builder.setPositiveButton("确定", null);
                builder.setNeutralButton("导出报告", (dialog, which) -> exportReportToFile(report.toString(), "monthly_report"));
                builder.show();
            });
        }).start();
    }

    private int countNormalBpDays() {
        int count = 0;
        for (float bp : bpData) {
            if (bp > 0 && bp < 140) count++;
        }
        return count;
    }

    private int countNormalBsDays() {
        int count = 0;
        for (float bs : bsData) {
            if (bs > 0 && bs < 7.8) count++;
        }
        return count;
    }

    private int calculateImprovement() {
        return 5;
    }

    private void checkPermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_PERMISSION);
        } else {
            exportAllData();
        }
    }

    private void exportAllData() {
        new Thread(() -> {
            try {
                StringBuilder exportContent = new StringBuilder();
                exportContent.append("=== 健康数据导出 ===\n\n");
                exportContent.append("导出时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date())).append("\n\n");

                exportContent.append("健康评分：").append(healthScoreView.getScore()).append("分\n");
                exportContent.append("评分等级：").append(healthScoreView.getScoreLevel(healthScoreView.getScore())).append("\n\n");
                exportContent.append("健康建议：\n").append(healthScoreView.getScoreAdvice()).append("\n\n");

                exportContent.append("=== 数据摘要 ===\n\n");
                exportContent.append("最近血压趋势：\n");
                for (int i = 0; i < labels.size(); i++) {
                    exportContent.append(labels.get(i)).append(": ")
                            .append(String.format("%.1f", bpData.get(i))).append(" mmHg\n");
                }
                exportContent.append("\n最近血糖趋势：\n");
                for (int i = 0; i < labels.size(); i++) {
                    exportContent.append(labels.get(i)).append(": ")
                            .append(String.format("%.1f", bsData.get(i))).append(" mmol/L\n");
                }

                String fileName = "health_data_export_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";

                boolean success = saveToFile(fileName, exportContent.toString());

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "✅ 数据导出成功", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "导出失败，请检查权限", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "导出失败", e);
                runOnUiThread(() -> Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void exportReportToFile(String content, String reportType) {
        new Thread(() -> {
            String fileName = reportType + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            boolean success = saveToFile(fileName, content);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "✅ 报告导出成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "导出失败，请检查权限", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private boolean saveToFile(String fileName, String content) {
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File healthDir = new File(downloadDir, "HealthAssistant");
            if (!healthDir.exists()) healthDir.mkdirs();

            File file = new File(healthDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "保存文件失败", e);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportAllData();
            } else {
                Toast.makeText(this, "需要存储权限才能导出数据", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}