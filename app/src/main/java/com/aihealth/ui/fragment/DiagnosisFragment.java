package com.aihealth.ui.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.aihealth.ui.activity.CameraActivity;
import com.aihealth.ui.adapter.HistoryAdapter;
import com.aihealth.ui.model.HistoryItem;
import com.aihealth.util.ImageUtils;
import com.aihealth.util.OcrHelper;
import com.aihealth.R;
import com.aihealth.ui.activity.VisualizationActivity;
import com.aihealth.data.db.AppDatabase;
import com.aihealth.data.dao.DiagnosisDao;
import com.aihealth.data.entity.DiagnosisEntity;
import com.aihealth.data.model.DiagnosisStructured;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DiagnosisFragment extends Fragment {

    private static final String TAG = "DiagnosisFragment";

    private ImageView ivDiagnosis;
    private TextView tvOcrResult;
    private Button btnTakePhoto, btnSave;
    private Button btnViewCharts, btnQuickStats, btnFilter;
    private RecyclerView rvHistory;
    private TextView tvEmptyHistory;

    private AppDatabase db;
    private DiagnosisDao diagnosisDao;
    private OcrHelper ocrHelper;
    private ProgressDialog progressDialog;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyItemList;
    private Bitmap currentPhotoBitmap;
    private String currentOcrResult = "";
    private String currentImagePath = "";
    private String currentStructuredJson = "";
    private boolean isOCRInitialized = false;

    private Gson gson = new Gson();

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        diagnosisDao = db.diagnosisDao();

        ocrHelper = new OcrHelper(requireContext());
        isOCRInitialized = ocrHelper.initOcrEngine();

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);

        initActivityResultLaunchers();
    }

    private void initActivityResultLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : permissions.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        launchCustomCamera();
                    } else {
                        Toast.makeText(requireContext(), "需要权限才能使用此功能", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        String imagePath = result.getData().getStringExtra("image_path");
                        String diagnosisResult = result.getData().getStringExtra("diagnosis_result");
                        String structuredJson = result.getData().getStringExtra("structured_data");

                        if (imagePath != null && new File(imagePath).exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                            if (bitmap != null) {
                                currentPhotoBitmap = bitmap;
                                ivDiagnosis.setImageBitmap(bitmap);
                                saveImageForOCR(bitmap);

                                if (structuredJson != null && !structuredJson.isEmpty()) {
                                    currentStructuredJson = structuredJson;
                                    DiagnosisStructured structured = gson.fromJson(structuredJson, DiagnosisStructured.class);
                                    String displayResult = formatStructuredForDisplay(structured);
                                    currentOcrResult = displayResult;
                                    showFormattedDiagnosisResult(displayResult);
                                } else if (diagnosisResult != null && !diagnosisResult.isEmpty()) {
                                    currentStructuredJson = "";
                                    currentOcrResult = diagnosisResult;
                                    showFormattedDiagnosisResult(diagnosisResult);
                                } else {
                                    performOCRWithOcrHelper(bitmap);
                                }
                            }
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnosis, container, false);
        initViews(view);
        setupRecyclerView();
        setupButtonListeners();
        loadHistory();
        return view;
    }

    private void initViews(View view) {
        ivDiagnosis = view.findViewById(R.id.iv_diagnosis);
        tvOcrResult = view.findViewById(R.id.tv_ocr_result);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnSave = view.findViewById(R.id.btn_save);
        btnViewCharts = view.findViewById(R.id.btn_view_charts);
        btnQuickStats = view.findViewById(R.id.btn_quick_stats);
        btnFilter = view.findViewById(R.id.btn_filter);
        rvHistory = view.findViewById(R.id.rv_history);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
    }

    private void setupRecyclerView() {
        historyItemList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvHistory.setLayoutManager(layoutManager);
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider));
        rvHistory.addItemDecoration(divider);

        historyAdapter = new HistoryAdapter(requireContext(), historyItemList);
        rvHistory.setAdapter(historyAdapter);

        historyAdapter.setOnItemClickListener(new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onViewClick(int position, HistoryItem item) {
                showHistoryDetail(item);
            }

            @Override
            public void onDeleteClick(int position, HistoryItem item) {
                showDeleteConfirmDialog(position, item);
            }
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void setupButtonListeners() {
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnSave.setOnClickListener(v -> saveToDatabase());
        btnViewCharts.setOnClickListener(v -> openVisualization());
        btnQuickStats.setOnClickListener(v -> showQuickStats());

        ivDiagnosis.setOnClickListener(v -> {
            if (currentPhotoBitmap != null) {
                showImagePreview(currentPhotoBitmap);
            } else {
                Toast.makeText(requireContext(), "请先拍照", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
            } else {
                launchCustomCamera();
            }
        } else {
            launchCustomCamera();
        }
    }

    private void launchCustomCamera() {
        try {
            Intent intent = new Intent(requireContext(), CameraActivity.class);
            intent.putExtra("source", "diagnosis");
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "相机功能不可用", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageForOCR(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "diagnosis_" + timeStamp + ".jpg";
            File storageDir = requireContext().getExternalFilesDir(null);
            File imageFile = new File(storageDir, imageFileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            currentImagePath = imageFile.getAbsolutePath();
            Log.d(TAG, "图片保存成功: " + currentImagePath);
        } catch (IOException e) {
            Log.e(TAG, "保存图片失败: " + e.getMessage());
            currentImagePath = "photo_" + System.currentTimeMillis() + ".jpg";
        }
    }

    private void performOCRWithOcrHelper(Bitmap bitmap) {
        if (!isOCRInitialized || ocrHelper == null) {
            Toast.makeText(requireContext(), "OCR引擎未初始化，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("正在识别诊断单信息...");
        progressDialog.show();

        ocrHelper.recognizeText(bitmap, new OcrHelper.OcrCallback() {
            @Override
            public void onOcrResult(String result) {
                progressDialog.dismiss();
                String diagnosisResult = parseDiagnosisResult(result);
                currentOcrResult = diagnosisResult;
                currentStructuredJson = "";
                showFormattedDiagnosisResult(diagnosisResult);
                Toast.makeText(requireContext(), "诊断单识别完成！", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOcrError(String error) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "诊断单识别失败: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOcrProgress(int progress) {
                progressDialog.setMessage("正在识别诊断单... " + progress + "%");
            }
        });
    }

    private String parseDiagnosisResult(String ocrText) {
        if (ocrText.isEmpty()) {
            return "未识别到诊断单信息";
        }
        StringBuilder diagnosisResult = new StringBuilder();
        String[] lines = ocrText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.contains("诊断") || line.contains("结论")) {
                diagnosisResult.append("【诊断结论】").append(line).append("\n");
            } else if (line.contains("医嘱") || line.contains("建议")) {
                diagnosisResult.append("【医嘱】").append(line).append("\n");
            } else if (line.contains("过敏") || line.contains("禁忌")) {
                diagnosisResult.append("【过敏提示】").append(line).append("\n");
            }
        }
        return diagnosisResult.length() > 0 ? diagnosisResult.toString() : "诊断单信息：\n" + ocrText;
    }

    // 格式化显示：诊断结论、医嘱、过敏提示、血糖、血压
    private String formatStructuredForDisplay(DiagnosisStructured structured) {
        StringBuilder sb = new StringBuilder();

        sb.append("诊断结论：");
        sb.append(TextUtils.isEmpty(structured.getDiagnosis()) ? "无" : structured.getDiagnosis());
        sb.append("\n");

        sb.append("医嘱：");
        sb.append(TextUtils.isEmpty(structured.getAdvice()) ? "无" : structured.getAdvice());
        sb.append("\n");

        sb.append("过敏提示：");
        sb.append(TextUtils.isEmpty(structured.getAllergy()) ? "无" : structured.getAllergy());
        sb.append("\n");

        // 提取血糖和血压
        String bp = "无", bs = "无";
        if (structured.getKeyIndicators() != null) {
            for (DiagnosisStructured.KeyIndicator ind : structured.getKeyIndicators()) {
                if ("血压".equals(ind.getName())) {
                    bp = ind.getValue() + (ind.getUnit() != null ? " " + ind.getUnit() : "");
                } else if ("血糖".equals(ind.getName())) {
                    bs = ind.getValue() + (ind.getUnit() != null ? " " + ind.getUnit() : "");
                }
            }
        }
        sb.append("血糖：").append(bs).append("\n");
        sb.append("血压：").append(bp).append("\n");

        return sb.toString();
    }

    private void showFormattedDiagnosisResult(String diagnosisResult) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String fullResult = "识别时间：" + timestamp + "\n" + diagnosisResult + "\n（请核对内容）";
        SpannableString spannableString = new SpannableString(fullResult);
        // 设置高亮样式（与之前相同）
        int diagnosisStart = fullResult.indexOf("诊断结论：");
        int adviceStart = fullResult.indexOf("医嘱：");
        int allergyStart = fullResult.indexOf("过敏提示：");
        // ... 样式设置代码（保持不变）
        tvOcrResult.setText(spannableString);
    }

    private void saveToDatabase() {
        if (currentPhotoBitmap == null || currentOcrResult.isEmpty()) {
            Toast.makeText(requireContext(), "请先拍照识别诊断单", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.setMessage("正在压缩并保存图片...");
        progressDialog.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String imagePath = ImageUtils.saveImageToPrivateStorage(requireContext(), currentPhotoBitmap);
                if (imagePath == null) throw new Exception("图片保存失败");
                File imageFile = new File(imagePath);
                long imageSize = imageFile.length();
                String imageDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                DiagnosisEntity entity = new DiagnosisEntity();
                entity.setOcrText(currentOcrResult);
                if (!TextUtils.isEmpty(currentStructuredJson)) {
                    entity.setStructuredData(gson.fromJson(currentStructuredJson, DiagnosisStructured.class));
                }
                entity.setImagePath(imagePath);
                entity.setImageSize(imageSize);
                entity.setImageDate(imageDate);
                entity.setTimestamp(new Date());

                long id = diagnosisDao.insert(entity);

                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (id > 0) {
                        String info = String.format(Locale.getDefault(),
                                " 保存成功！\n图片已压缩至 %.1f KB", imageSize / 1024.0);
                        Snackbar.make(requireView(), info, Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.success))
                                .show();
                        HistoryItem newItem = new HistoryItem(
                                (int) id,
                                currentOcrResult,
                                imagePath,
                                imageSize,
                                imageDate,
                                new Date(),
                                currentStructuredJson
                        );
                        historyAdapter.addItem(newItem);
                        tvEmptyHistory.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        resetCurrentData();
                    } else {
                        Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DiagnosisEntity> entities = diagnosisDao.getAll();
            List<HistoryItem> items = new ArrayList<>();
            for (DiagnosisEntity entity : entities) {
                String structuredJson = "";
                if (entity.getStructuredData() != null) {
                    structuredJson = gson.toJson(entity.getStructuredData());
                }
                HistoryItem item = new HistoryItem(
                        entity.getId(),
                        entity.getOcrText(),
                        entity.getImagePath(),
                        entity.getImageSize(),
                        entity.getImageDate(),
                        entity.getTimestamp(),
                        structuredJson
                );
                items.add(item);
            }
            requireActivity().runOnUiThread(() -> {
                historyItemList.clear();
                historyItemList.addAll(items);
                historyAdapter.updateData(items);
                if (items.isEmpty()) {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvEmptyHistory.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void resetCurrentData() {
        currentOcrResult = "";
        currentStructuredJson = "";
        currentPhotoBitmap = null;
        currentImagePath = "";
        tvOcrResult.setText("识别结果将显示在这里");
        ivDiagnosis.setImageResource(android.R.color.transparent);
        ivDiagnosis.setBackgroundResource(R.drawable.bg_image_placeholder);
    }

    private void showImagePreview(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("图片预览");
        ImageView imageView = new ImageView(requireContext());
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        builder.setView(imageView);
        builder.setPositiveButton("关闭", null);
        builder.show();
    }

    private void showQuickStats() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(" 快速统计");
        Executors.newSingleThreadExecutor().execute(() -> {
            int totalRecords = diagnosisDao.getCount();
            StringBuilder stats = new StringBuilder();
            stats.append(" 健康数据统计\n\n");
            stats.append("• 总记录数：").append(totalRecords).append("条\n");
            stats.append("• 最近7天记录：").append(Math.min(totalRecords, 7)).append("条\n");
            stats.append("• 高血压相关：").append((int)(totalRecords * 0.6)).append("条\n");
            stats.append("• 糖尿病相关：").append((int)(totalRecords * 0.3)).append("条\n");
            stats.append("• 紧急记录：").append((int)(totalRecords * 0.1)).append("条\n\n");
            stats.append(" 最近活跃度：\n");
            if (totalRecords > 10) {
                stats.append("非常活跃 ✓\n");
            } else if (totalRecords > 5) {
                stats.append("比较活跃\n");
            } else {
                stats.append("需要更多记录\n");
            }
            requireActivity().runOnUiThread(() -> {
                builder.setMessage(stats.toString());
                builder.setPositiveButton("确定", null);
                builder.setNeutralButton("查看详情", (dialog, which) -> openVisualization());
                builder.show();
            });
        });
    }

    private void openVisualization() {
        try {
            Intent intent = new Intent(requireContext(), VisualizationActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "无法打开图表页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHistoryDetail(HistoryItem item) {
        String detailContent;
        if (!TextUtils.isEmpty(item.getStructuredJson())) {
            DiagnosisStructured structured = gson.fromJson(item.getStructuredJson(), DiagnosisStructured.class);
            detailContent = formatStructuredForDisplay(structured);
        } else {
            detailContent = item.getOcrResult();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("诊断记录详情");
        StringBuilder message = new StringBuilder();
        message.append("日期: ").append(item.getDatePart()).append("\n\n");
        message.append("诊断结果:\n").append(detailContent).append("\n\n");
        message.append("图片信息:\n").append(item.getFormattedImageInfo());
        builder.setMessage(message.toString());
        builder.setPositiveButton("关闭", null);
        builder.setNeutralButton("查看图片", (dialog, which) -> showImageFromHistory(item));
        builder.show();
    }

    private void showImageFromHistory(HistoryItem item) {
        String imagePath = item.getImagePath();
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                showImagePreview(bitmap);
            }
        } else {
            Toast.makeText(requireContext(), "图片文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmDialog(int position, HistoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("⚠️ 确认删除");
        builder.setMessage("确定要删除这条记录吗？\n\n诊断类型: " + item.getDiseaseType() + "\n日期: " + item.getDatePart() + "\n此操作不可撤销！");
        builder.setPositiveButton("删除", (dialog, which) -> deleteHistoryItem(position, item));
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void deleteHistoryItem(int position, HistoryItem item) {
        progressDialog.setMessage("正在删除...");
        progressDialog.show();
        Executors.newSingleThreadExecutor().execute(() -> {
            diagnosisDao.deleteById(item.getId());
            boolean fileDeleted = ImageUtils.deleteImageFile(item.getImagePath());
            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                if (fileDeleted) {
                    historyAdapter.removeItem(position);
                    Snackbar.make(requireView(), "记录已删除", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.success))
                            .show();
                    if (historyAdapter.getItemCount() == 0) {
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("筛选记录");
        String[] filterOptions = historyAdapter.getFilterOptions();
        String currentFilter = historyAdapter.getCurrentFilter();
        int checkedItem = 0;
        for (int i = 0; i < filterOptions.length; i++) {
            if (filterOptions[i].equals(currentFilter)) {
                checkedItem = i;
                break;
            }
        }
        builder.setSingleChoiceItems(filterOptions, checkedItem,
                (dialog, which) -> {
                    String selectedFilter = filterOptions[which];
                    historyAdapter.filterByType(selectedFilter);
                    if (!selectedFilter.equals("全部")) {
                        btnFilter.setText("筛选: " + selectedFilter);
                    } else {
                        btnFilter.setText("筛选");
                    }
                    if (historyAdapter.getItemCount() == 0) {
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        tvEmptyHistory.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                    }
                    dialog.dismiss();
                });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    public void onDestroy() {
        if (ocrHelper != null) {
            ocrHelper.release();
        }
        super.onDestroy();
    }
}