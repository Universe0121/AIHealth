package com.oppo.AIHealth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.oppo.AIHealth.data.AppDatabaseA;
import com.oppo.AIHealth.data.AppDatabaseC;
import com.oppo.AIHealth.data.Drug;
import com.oppo.AIHealth.model.DietRecord;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DietAnalysisActivity extends AppCompatActivity {

    private ImageView ivFoodImage;
    private TextView tvFoodItems, tvCalories, tvNutritionAnalysis, tvDrugFoodConflict;
    private PieChart pieChart;
    private Button btnSave;
    private RecyclerView rvHistory;
    private View layoutAnalysis, layoutHistory;

    private String imagePath;
    private String recognizedFoods;
    private double totalCalories;

    // 数据库
    private AppDatabaseA dbA;
    private AppDatabaseC dbC;

    // 历史记录适配器
    private DietHistoryAdapter historyAdapter;
    private List<DietRecord> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_analysis);

        dbA = AppDatabaseA.getInstance(this);
        dbC = AppDatabaseC.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        setupChart();

        boolean showHistory = getIntent().getBooleanExtra("show_history", false);

        if (showHistory) {
            showHistoryMode();
        } else {
            showAnalysisMode();
            imagePath = getIntent().getStringExtra("image_path");
            analyzeFoodImage();
        }
    }

    private void initViews() {
        // 分析模式视图
        ivFoodImage = findViewById(R.id.iv_food_image);
        tvFoodItems = findViewById(R.id.tv_food_items);
        tvCalories = findViewById(R.id.tv_calories);
        tvNutritionAnalysis = findViewById(R.id.tv_nutrition_analysis);
        tvDrugFoodConflict = findViewById(R.id.tv_drug_food_conflict);
        pieChart = findViewById(R.id.pie_chart);
        btnSave = findViewById(R.id.btn_save);

        // 历史模式视图
        rvHistory = findViewById(R.id.rv_history);
        layoutAnalysis = findViewById(R.id.layout_analysis);
        layoutHistory = findViewById(R.id.layout_history);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(DietAnalysisActivity.this, MainActivity.class));
            finish();
        });

        btnSave.setOnClickListener(v -> saveDietRecord());

        // 设置RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new DietHistoryAdapter(historyList, this::showHistoryDetail);
        rvHistory.setAdapter(historyAdapter);

        // Toolbar导航
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupChart() {
        if (pieChart == null) return;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.white);
        pieChart.setTransparentCircleRadius(61f);
    }

    private void showAnalysisMode() {
        if (layoutAnalysis != null) layoutAnalysis.setVisibility(View.VISIBLE);
        if (layoutHistory != null) layoutHistory.setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("饮食分析");
        }
    }

    private void showHistoryMode() {
        if (layoutAnalysis != null) layoutAnalysis.setVisibility(View.GONE);
        if (layoutHistory != null) layoutHistory.setVisibility(View.VISIBLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("历史记录");
        }
        loadHistoryData();
    }

    private void analyzeFoodImage() {
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    ivFoodImage.setImageBitmap(bitmap);
                    recognizedFoods = com.oppo.AIHealth.utils.FoodRecognitionHelper.recognizeFoodFromImage(bitmap, this);
                } else {
                    recognizedFoods = "米饭,鸡胸肉,西兰花";
                }
            } else {
                recognizedFoods = "米饭,鸡胸肉,西兰花";
            }
        } else {
            recognizedFoods = "米饭,鸡胸肉,西兰花";
        }

        totalCalories = com.oppo.AIHealth.utils.FoodRecognitionHelper.calculateCalories(recognizedFoods, this);

        tvFoodItems.setText("识别食材：" + recognizedFoods);
        tvCalories.setText(String.format(Locale.US, "热量估算：%.2f 千卡", totalCalories));
        tvNutritionAnalysis.setText(getNutritionAnalysis(recognizedFoods));
        tvDrugFoodConflict.setText(getDrugFoodConflict(recognizedFoods));

        updateChart();
        checkDrugFoodConflict(recognizedFoods);
    }

    private void updateChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(40f, "碳水化合物"));
        entries.add(new PieEntry(30f, "蛋白质"));
        entries.add(new PieEntry(20f, "脂肪"));
        entries.add(new PieEntry(10f, "其他"));

        PieDataSet dataSet = new PieDataSet(entries, "营养分布");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(11f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void saveDietRecord() {
        if (recognizedFoods == null) {
            Toast.makeText(this, "请先分析食物", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                DietRecord record = new DietRecord();
                record.setFoodItems(recognizedFoods);
                record.setCalories(totalCalories);
                record.setImagePath(imagePath);

                dbC.dietDao().insertDietRecord(record);

                runOnUiThread(() -> {
                    Toast.makeText(this, "记录保存成功", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadHistoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DietRecord> records = dbC.dietDao().getAllDietRecords();
            runOnUiThread(() -> {
                historyList.clear();
                historyList.addAll(records);
                historyAdapter.notifyDataSetChanged();
            });
        });
    }

    private void showHistoryDetail(DietRecord record) {
        // 弹窗显示详情
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("饮食记录详情");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String timeStr = sdf.format(record.getTimestamp());

        String message = "时间：" + timeStr + "\n\n" +
                "食物：" + record.getFoodItems() + "\n" +
                String.format("热量：%.2f 千卡", record.getCalories());

        builder.setMessage(message);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    private String getNutritionAnalysis(String foodItems) {
        if (foodItems.contains("宫保鸡丁")) {
            return "宫保鸡丁营养成分分析：\n- 热量：约180千卡/100克\n- 蛋白质：约15克/100克\n- 脂肪：约10克/100克\n- 碳水化合物：约8克/100克\n- 维生素：维生素A、C、E\n- 矿物质：钙、铁、锌\n- 特点：富含优质蛋白质，适合健身人群。";
        } else if (foodItems.contains("鱼香肉丝")) {
            return "鱼香肉丝营养成分分析：\n- 热量：约150千卡/100克\n- 蛋白质：约12克/100克\n- 脂肪：约8克/100克\n- 碳水化合物：约10克/100克\n- 维生素：B1、B2、烟酸\n- 矿物质：钙、磷、铁\n- 特点：酸甜可口，营养均衡。";
        } else {
            return "暂无营养成分分析";
        }
    }

    private String getDrugFoodConflict(String foodItems) {
        if (foodItems.contains("宫保鸡丁")) {
            return "宫保鸡丁药食冲突提醒：\n- 服用布洛芬期间：辣椒会刺激胃黏膜，与药物叠加容易增加胃出血风险。建议尽量避免吃太辣的宫保鸡丁。";
        } else if (foodItems.contains("鱼香肉丝")) {
            return "鱼香肉丝药食冲突提醒：\n- 服用胰岛素期间：鱼香肉丝高糖，会导致血糖迅速升高。建议糖尿病患者避免食用过甜的鱼香肉丝。\n- 服用单胺氧化酶抑制剂期间：豆瓣酱含酪胺，可能导致血压骤升。建议避免食用含大量发酵酱料的菜肴。";
        } else {
            return "暂无药食冲突提醒";
        }
    }

    private void checkDrugFoodConflict(String foodItems) {
        if (!foodItems.contains("宫保鸡丁")) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Drug> drugs = dbA.appDao().getAllDrugs();
            boolean[] hasDrugs = new boolean[]{false, false};

            for (Drug drug : drugs) {
                String name = drug.getDrugName().toLowerCase();
                if (name.contains("布洛芬") || name.contains("ibuprofen")) {
                    hasDrugs[0] = true;
                }
                if (name.contains("阿司匹林") || name.contains("aspirin")) {
                    hasDrugs[1] = true;
                }
            }

            if (hasDrugs[0] || hasDrugs[1]) {
                runOnUiThread(() -> {
                    String drugNames = "";
                    if (hasDrugs[0]) drugNames += "布洛芬";
                    if (hasDrugs[0] && hasDrugs[1]) drugNames += "和";
                    if (hasDrugs[1]) drugNames += "阿司匹林";

                    new AlertDialog.Builder(DietAnalysisActivity.this)
                            .setTitle("⚠️ 药物-食物冲突警告")
                            .setMessage("您正在服用" + drugNames +
                                    "，而宫保鸡丁中的辣椒可能刺激胃黏膜，增加胃出血风险。建议避免食用太辣的宫保鸡丁。")
                            .setPositiveButton("已知晓", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                });
            }
        });
    }
}