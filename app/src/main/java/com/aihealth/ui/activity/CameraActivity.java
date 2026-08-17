package com.aihealth.ui.activity;

import com.aihealth.R;

import androidx.core.content.FileProvider;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.aihealth.receiver.DrugCycleReceiver;
import com.aihealth.data.db.AppDatabase;
import com.aihealth.data.dao.DiagnosisDao;
import com.aihealth.data.entity.DiagnosisEntity;
import com.aihealth.data.model.DiagnosisStructured;
import com.aihealth.data.entity.Drug;
import com.aihealth.util.OcrDiagnosisParser;
import com.aihealth.util.OcrDrugParser;
import com.aihealth.util.OcrHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private String source;
    private String currentPhotoPath;
    private OcrHelper ocrHelper;
    private AppDatabase db;

    // 药品管理模式控件
    private TextView tvOcrResult;
    private EditText etDrugName, etTakeTimes;
    private Button btnSetTakeTime, btnConfirmSave, btnRetake;
    private Calendar takeTime;
    private List<String> ocrResultList;
    private boolean isMultiShotMode = false;
    private List<Calendar> takeTimeList = new ArrayList<>();

    // 诊断单临时数据
    private List<String> diagnosisOcrResultList = new ArrayList<>();
    private OcrDiagnosisParser.Result currentDiagnosisTemp;

    // Gson
    private Gson gson = new Gson();

    // 磺胺类药物关键词列表
    private static final String[] SULFA_DRUG_KEYWORDS = {
            "磺胺", "SMZ", "磺胺嘧啶", "磺胺甲噁唑", "复方新诺明", "磺胺米隆", "磺胺醋酰", "磺胺多辛"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ocrHelper = new OcrHelper();
        ocrHelper.initOcrEngine();
        db = AppDatabase.getInstance(getApplicationContext());

        source = getIntent().getStringExtra("source");
        if (source == null) {
            source = "diet_analysis";
        }

        ocrResultList = new ArrayList<>();
        takeTime = Calendar.getInstance();
        takeTime.set(Calendar.HOUR_OF_DAY, 8);
        takeTime.set(Calendar.MINUTE, 0);
        takeTime.set(Calendar.SECOND, 0);
        takeTimeList = new ArrayList<>();

        Log.d(TAG, "CameraActivity started with source: " + source);

        initDrugManagementViews();

        findViewById(R.id.btn_close).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        MaterialCardView btnCapture = findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(v -> {
            Log.d(TAG, "Capture button clicked");
            if ("drug_management".equals(source) && isMultiShotMode) {
                Toast.makeText(this, "继续拍摄补充药品信息...", Toast.LENGTH_SHORT).show();
            } else if ("diagnosis".equals(source) && isMultiShotMode) {
                Toast.makeText(this, "继续拍摄补充诊断单信息...", Toast.LENGTH_SHORT).show();
            }
            openCamera();
        });
    }

    /**
     * 初始化药品管理模式专属控件
     */
    private void initDrugManagementViews() {
        View nsvDrugEditor = findViewById(R.id.nsv_drug_editor);

        tvOcrResult = findViewById(R.id.tv_ocr_result);
        etDrugName = findViewById(R.id.et_drug_name_ocr);
        etTakeTimes = findViewById(R.id.et_take_times_ocr);
        btnSetTakeTime = findViewById(R.id.btn_set_take_time_ocr);
        btnConfirmSave = findViewById(R.id.btn_confirm_save);
        btnRetake = findViewById(R.id.btn_retake);

        if ("drug_management".equals(source)) {
            if (nsvDrugEditor != null) nsvDrugEditor.setVisibility(View.GONE);
            if (tvOcrResult != null) tvOcrResult.setVisibility(View.GONE);
            if (etDrugName != null) etDrugName.setVisibility(View.GONE);
            if (etTakeTimes != null) etTakeTimes.setVisibility(View.GONE);
            if (btnSetTakeTime != null) {
                btnSetTakeTime.setVisibility(View.GONE);
                btnSetTakeTime.setText("设置服用时间: 08:00");
                btnSetTakeTime.setOnClickListener(v -> showTimePickerDialog());
            }
            if (btnConfirmSave != null) {
                btnConfirmSave.setVisibility(View.GONE);
                btnConfirmSave.setOnClickListener(v -> saveDrugFromOcr());
            }
            if (btnRetake != null) {
                btnRetake.setVisibility(View.GONE);
                btnRetake.setOnClickListener(v -> {
                    ocrResultList.clear();
                    isMultiShotMode = false;
                    hideDrugEditViews();
                    openCamera();
                });
            }
        } else {
            if (nsvDrugEditor != null) nsvDrugEditor.setVisibility(View.GONE);
            if (tvOcrResult != null) tvOcrResult.setVisibility(View.GONE);
            if (etDrugName != null) etDrugName.setVisibility(View.GONE);
            if (etTakeTimes != null) etTakeTimes.setVisibility(View.GONE);
            if (btnSetTakeTime != null) btnSetTakeTime.setVisibility(View.GONE);
            if (btnConfirmSave != null) btnConfirmSave.setVisibility(View.GONE);
            if (btnRetake != null) btnRetake.setVisibility(View.GONE);
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void openCamera() {
        checkCameraPermission();
    }

    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    currentPhotoPath = photoFile.getAbsolutePath();
                    Log.d(TAG, "Photo file created at: " + currentPhotoPath);

                    Uri photoUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        photoUri = FileProvider.getUriForFile(
                                this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Log.e(TAG, "Failed to create image file");
                    Toast.makeText(this, "无法创建图片文件", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                showCameraNotFoundDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera: " + e.getMessage(), e);
            Toast.makeText(this, "打开相机失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Photo captured successfully");
                handlePhotoCaptured();
            } else {
                Log.d(TAG, "Photo capture cancelled");
                if ("drug_management".equals(source)) {
                    finish();
                } else if ("diagnosis".equals(source)) {
                    finish();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    private void handlePhotoCaptured() {
        if (currentPhotoPath == null) {
            Log.e(TAG, "currentPhotoPath is null");
            Toast.makeText(this, "照片保存失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File photoFile = new File(currentPhotoPath);
        if (!photoFile.exists()) {
            Log.e(TAG, "Photo file does not exist: " + currentPhotoPath);
            Toast.makeText(this, "照片文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Photo exists at: " + currentPhotoPath + ", size: " + photoFile.length());

        if ("diagnosis".equals(source)) {
            processDiagnosisSheetPhoto();
        } else if ("drug_management".equals(source)) {
            processDrugManagementPhoto();
        } else {
            startDietAnalysisActivity();
        }
    }

    // ========== 诊断单模式处理逻辑 ==========
    private void processDiagnosisSheetPhoto() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        if (bitmap == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "开始识别诊断单信息...", Toast.LENGTH_SHORT).show();
        ocrHelper.recognizeText(bitmap, new OcrHelper.OcrCallback() {
            @Override
            public void onOcrResult(String result) {
                Log.d(TAG, "诊断单OCR识别结果：" + result);
                diagnosisOcrResultList.add(result);

                String mergedResult = OcrDiagnosisParser.mergeResults(diagnosisOcrResultList);
                OcrDiagnosisParser.Result temp = OcrDiagnosisParser.parse(mergedResult);
                currentDiagnosisTemp = temp;

                List<String> missingList = temp.getMissingFields();

                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this,
                            "已拍摄" + diagnosisOcrResultList.size() + "张，识别完成",
                            Toast.LENGTH_SHORT).show();

                    if (!missingList.isEmpty()) {
                        StringBuilder missingMsg = new StringBuilder();
                        missingMsg.append("当前未识别到以下内容：\n");
                        for (String item : missingList) {
                            missingMsg.append("• ").append(item).append("\n");
                        }
                        missingMsg.append("\n是否继续拍摄补充信息？");

                        new AlertDialog.Builder(CameraActivity.this)
                                .setTitle("信息不完整")
                                .setMessage(missingMsg.toString())
                                .setPositiveButton("继续拍摄", (dialog, which) -> {
                                    isMultiShotMode = true;
                                    openCamera();
                                })
                                .setNegativeButton("手动编辑", (dialog, which) -> {
                                    showDiagnosisEditDialog(temp);
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        returnDiagnosisResult(temp);
                    }
                });
            }

            @Override
            public void onOcrError(String error) {
                Log.e(TAG, "诊断单OCR识别失败：" + error);
                new AlertDialog.Builder(CameraActivity.this)
                        .setTitle("识别失败")
                        .setMessage("OCR识别失败：" + error + "\n是否手动输入诊断信息？")
                        .setPositiveButton("手动输入", (dialog, which) -> {
                            showDiagnosisEditDialog(new OcrDiagnosisParser.Result());
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            finish();
                        })
                        .show();
            }

            @Override
            public void onOcrProgress(int progress) {
                Log.d(TAG, "诊断单识别进度：" + progress + "%");
            }
        });
    }





    private void showDiagnosisEditDialog(OcrDiagnosisParser.Result temp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑诊断单信息");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_diagnosis_edit, null);
        EditText etDiagnosis = view.findViewById(R.id.et_diagnosis);
        EditText etAdvice = view.findViewById(R.id.et_advice);
        EditText etAllergy = view.findViewById(R.id.et_allergy);
        EditText etBloodPressure = view.findViewById(R.id.et_blood_pressure);
        EditText etBloodSugar = view.findViewById(R.id.et_blood_sugar);

        etDiagnosis.setText(temp.diagnosis);
        etAdvice.setText(temp.advice);
        etAllergy.setText(temp.allergy);
        etBloodPressure.setText(temp.bloodPressure);
        etBloodSugar.setText(temp.bloodSugar);

        builder.setView(view);
        builder.setPositiveButton("确定", (dialog, which) -> {
            temp.diagnosis = etDiagnosis.getText().toString().trim();
            temp.advice = etAdvice.getText().toString().trim();
            temp.allergy = etAllergy.getText().toString().trim();
            temp.bloodPressure = etBloodPressure.getText().toString().trim();
            temp.bloodSugar = etBloodSugar.getText().toString().trim();
            returnDiagnosisResult(temp);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void returnDiagnosisResult(OcrDiagnosisParser.Result temp) {
        DiagnosisStructured structured = temp.toStructured();
        String structuredJson = gson.toJson(structured);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("image_path", currentPhotoPath);
        resultIntent.putExtra("diagnosis_result", temp.diagnosis);
        resultIntent.putExtra("structured_data", structuredJson);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // ========== 药品管理模式处理逻辑（添加过敏检测） ==========
    private void processDrugManagementPhoto() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        if (bitmap == null) {
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "开始识别药品信息...", Toast.LENGTH_SHORT).show();
        ocrHelper.recognizeText(bitmap, new OcrHelper.OcrCallback() {
            @Override
            public void onOcrResult(String result) {
                Log.d(TAG, "药品管理OCR识别结果：" + result);
                ocrResultList.add(result);

                String mergedResult = OcrDrugParser.mergeResults(ocrResultList);
                Log.d(TAG, "多图合并结果：" + mergedResult);

                Drug tempDrug = OcrDrugParser.parse(mergedResult);
                List<String> missingList = OcrDrugParser.findMissingFields(tempDrug);
                String showResult = OcrDrugParser.formatResult(tempDrug);

                // 过敏检测（新增）
                checkAllergyConflict(tempDrug.getDrugName());

                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this,
                            "已拍摄" + ocrResultList.size() + "张，识别完成",
                            Toast.LENGTH_SHORT).show();

                    if (!missingList.isEmpty()) {
                        StringBuilder missingMsg = new StringBuilder();
                        missingMsg.append("当前未识别到以下内容：\n");
                        for (String item : missingList) {
                            missingMsg.append("• ").append(item).append("\n");
                        }
                        missingMsg.append("\n是否继续拍摄补充信息？");

                        new AlertDialog.Builder(CameraActivity.this)
                                .setTitle("信息不完整")
                                .setMessage(missingMsg.toString())
                                .setPositiveButton("继续拍摄", (dialog, which) -> {
                                    isMultiShotMode = true;
                                    openCamera();
                                })
                                .setNegativeButton("手动编辑", (dialog, which) -> {
                                    showDrugEditViews(tempDrug, showResult);
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        showDrugEditViews(tempDrug, showResult);
                    }
                });
            }

            @Override
            public void onOcrError(String error) {
                Log.e(TAG, "药品管理OCR识别失败：" + error);
                new AlertDialog.Builder(CameraActivity.this)
                        .setTitle("识别失败")
                        .setMessage("OCR识别失败：" + error + "\n是否手动输入药品信息？")
                        .setPositiveButton("手动输入", (dialog, which) -> {
                            Drug emptyDrug = new Drug();
                            emptyDrug.setDrugName("");
                            emptyDrug.setTakeTimes(1);
                            showDrugEditViews(emptyDrug, "未识别到药品信息，请手动输入");
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            finish();
                        })
                        .show();
            }

            @Override
            public void onOcrProgress(int progress) {
                Log.d(TAG, "识别进度：" + progress + "%");
            }
        });
    }

    // 新增：检查药品过敏冲突
    private void checkAllergyConflict(String drugName) {
        if (TextUtils.isEmpty(drugName)) return;

        // 获取诊断数据库中的过敏信息
        DiagnosisDao diagnosisDao = db.diagnosisDao();
        List<DiagnosisEntity> diagnoses = diagnosisDao.getAll(); // 获取所有诊断记录
        String allergyText = "";
        for (DiagnosisEntity diag : diagnoses) {
            DiagnosisStructured structured = diag.getStructuredData();
            if (structured != null && !TextUtils.isEmpty(structured.getAllergy())) {
                allergyText = structured.getAllergy();
                break; // 取最近一条有过敏信息的记录（可根据需要调整）
            }
        }

        if (TextUtils.isEmpty(allergyText)) return; // 无过敏信息

        // 判断药品是否属于磺胺类
        boolean isSulfa = false;
        String lowerDrug = drugName.toLowerCase();
        for (String keyword : SULFA_DRUG_KEYWORDS) {
            if (lowerDrug.contains(keyword.toLowerCase())) {
                isSulfa = true;
                break;
            }
        }

        if (isSulfa && allergyText.toLowerCase().contains("磺胺")) {
            runOnUiThread(() -> {
                new AlertDialog.Builder(CameraActivity.this)
                        .setTitle("⚠️ 过敏警告")
                        .setMessage("您有磺胺类药物过敏史，当前药品可能引起过敏反应！请谨慎使用或咨询医生。")
                        .setPositiveButton("已知晓", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            });
        }
    }



    private void showDrugEditViews(Drug drug, String ocrResult) {
        View nsvDrugEditor = findViewById(R.id.nsv_drug_editor);
        if (nsvDrugEditor != null) nsvDrugEditor.setVisibility(View.VISIBLE);

        if (tvOcrResult != null) {
            tvOcrResult.setVisibility(View.VISIBLE);
            tvOcrResult.setText(ocrResult);
        }

        if (etDrugName != null) {
            etDrugName.setVisibility(View.VISIBLE);
            etDrugName.setText(drug != null ? drug.getDrugName() : "");
            etDrugName.requestFocus();
        }

        if (etTakeTimes != null) {
            etTakeTimes.setVisibility(View.VISIBLE);
            int takeTimes = drug != null ? drug.getTakeTimes() : 1;
            etTakeTimes.setText(String.valueOf(takeTimes));
        }

        if (btnSetTakeTime != null) {
            btnSetTakeTime.setVisibility(View.VISIBLE);
            int times = drug != null ? drug.getTakeTimes() : 1;
            btnSetTakeTime.setText("设置服用时间（共" + times + "个）: 08:00");
            btnSetTakeTime.setOnClickListener(v -> showTimePickerDialog());
        }
        if (btnConfirmSave != null) {
            btnConfirmSave.setVisibility(View.VISIBLE);
            btnConfirmSave.setOnClickListener(v -> saveDrugFromOcr());
        }
        if (btnRetake != null) {
            btnRetake.setVisibility(View.VISIBLE);
            btnRetake.setOnClickListener(v -> {
                ocrResultList.clear();
                isMultiShotMode = false;
                takeTimeList.clear();
                hideDrugEditViews();
                openCamera();
            });
        }

        MaterialCardView btnCapture = findViewById(R.id.btn_capture);
        if (btnCapture != null) btnCapture.setVisibility(View.GONE);
    }

    private void hideDrugEditViews() {
        View nsvDrugEditor = findViewById(R.id.nsv_drug_editor);
        if (nsvDrugEditor != null) nsvDrugEditor.setVisibility(View.GONE);

        if (tvOcrResult != null) tvOcrResult.setVisibility(View.GONE);
        if (etDrugName != null) etDrugName.setVisibility(View.GONE);
        if (etTakeTimes != null) etTakeTimes.setVisibility(View.GONE);
        if (btnSetTakeTime != null) btnSetTakeTime.setVisibility(View.GONE);
        if (btnConfirmSave != null) btnConfirmSave.setVisibility(View.GONE);
        if (btnRetake != null) btnRetake.setVisibility(View.GONE);

        MaterialCardView btnCapture = findViewById(R.id.btn_capture);
        if (btnCapture != null) btnCapture.setVisibility(View.VISIBLE);
    }

    private void showTimePickerDialog() {
        int hour = takeTime.get(Calendar.HOUR_OF_DAY);
        int minute = takeTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    Calendar newTime = Calendar.getInstance();
                    newTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    newTime.set(Calendar.MINUTE, minute1);
                    newTime.set(Calendar.SECOND, 0);
                    takeTimeList.add(newTime);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    StringBuilder timeStr = new StringBuilder();
                    for (int i = 0; i < takeTimeList.size(); i++) {
                        if (i > 0) timeStr.append("、");
                        timeStr.append(timeFormat.format(takeTimeList.get(i).getTime()));
                    }
                    btnSetTakeTime.setText("已添加" + takeTimeList.size() + "个时间：" + timeStr);
                    Toast.makeText(this, "已添加服用时间: " + timeFormat.format(newTime.getTime()), Toast.LENGTH_SHORT).show();
                },
                hour, minute, true
        );
        timePickerDialog.setTitle("选择服用时间（可多次添加）");
        timePickerDialog.show();
    }

    private void saveDrugFromOcr() {
        String drugName = etDrugName.getText().toString().trim();
        String takeTimesStr = etTakeTimes.getText().toString().trim();

        if (TextUtils.isEmpty(drugName)) {
            etDrugName.setError("请输入药品名称");
            etDrugName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(takeTimesStr)) {
            etTakeTimes.setError("请输入每日服用次数");
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

        List<Calendar> finalTimeList = new ArrayList<>(takeTimeList);
        if (finalTimeList.isEmpty()) {
            Calendar defaultTime = Calendar.getInstance();
            defaultTime.set(Calendar.HOUR_OF_DAY, 8);
            defaultTime.set(Calendar.MINUTE, 0);
            defaultTime.set(Calendar.SECOND, 0);
            finalTimeList.add(defaultTime);
        }
        while (finalTimeList.size() < takeTimes) {
            Calendar defaultTime = Calendar.getInstance();
            switch (finalTimeList.size()) {
                case 0: defaultTime.set(Calendar.HOUR_OF_DAY, 8); break;
                case 1: defaultTime.set(Calendar.HOUR_OF_DAY, 12); break;
                case 2: defaultTime.set(Calendar.HOUR_OF_DAY, 18); break;
                case 3: defaultTime.set(Calendar.HOUR_OF_DAY, 22); break;
                default: defaultTime.set(Calendar.HOUR_OF_DAY, 8 + finalTimeList.size() * 4); break;
            }
            defaultTime.set(Calendar.MINUTE, 0);
            defaultTime.set(Calendar.SECOND, 0);
            finalTimeList.add(defaultTime);
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String firstTime = timeFormat.format(finalTimeList.get(0).getTime());

        List<String> timeStrList = new ArrayList<>();
        for (Calendar cal : finalTimeList) {
            timeStrList.add(timeFormat.format(cal.getTime()));
        }

        Drug drug = new Drug();
        drug.setDrugName(drugName);
        drug.setTakeTime(firstTime);
        drug.setTakeTimes(takeTimes);
        drug.setTakeStatus("未服用");
        drug.setTakeTimeListFromList(timeStrList);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        drug.setCreateTime(dateFormat.format(new Date()));

        new Thread(() -> {
            db.appDao().insertDrug(drug);
            drug.setId((int) System.currentTimeMillis());

            for (int i = 0; i < finalTimeList.size(); i++) {
                setDrugReminder(drug, finalTimeList.get(i), i);
            }

            runOnUiThread(() -> {
                Toast.makeText(CameraActivity.this, "药品添加成功，已设置" + finalTimeList.size() + "个提醒", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("drug_id", drug.getId());
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }

    private void setDrugReminder(Drug drug, Calendar time, int requestCodeOffset) {
        try {
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            reminderTime.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            reminderTime.set(Calendar.SECOND, 0);

            if (reminderTime.getTimeInMillis() <= System.currentTimeMillis()) {
                reminderTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null, cannot set reminder");
                return;
            }

            Intent intent = new Intent(this, DrugCycleReceiver.class);
            intent.putExtra("drug_id", drug.getId());
            intent.putExtra("drug_name", drug.getDrugName());
            intent.putExtra("take_time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time.getTime()));

            int requestCode = drug.getId() + requestCodeOffset;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
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

            Log.d(TAG, "药品提醒已设置: " + drug.getDrugName() + " 时间: " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time.getTime()));
        } catch (Exception e) {
            Log.e(TAG, "设置提醒失败: " + e.getMessage());
        }
    }




    // ========== 饮食分析模式 ==========
    private void startDietAnalysisActivity() {
        try {
            String[] sampleFoods = {"米饭", "鸡胸肉", "鸡蛋", "西兰花", "胡萝卜", "面包", "苹果"};
            Random random = new Random();
            int foodCount = random.nextInt(3) + 1;

            StringBuilder foodItemsBuilder = new StringBuilder();
            for (int i = 0; i < foodCount; i++) {
                if (i > 0) foodItemsBuilder.append(",");
                foodItemsBuilder.append(sampleFoods[random.nextInt(sampleFoods.length)]);
            }
            String foodItems = foodItemsBuilder.toString();

            double calories = 0;
            String[] foods = foodItems.split(",");
            for (String food : foods) {
                double baseCalories = getBaseCalories(food.trim());
                double weight = 100 + random.nextInt(200);
                calories += (baseCalories * weight / 100);
            }

            Intent intent = new Intent(CameraActivity.this, DietAnalysisActivity.class);
            intent.putExtra("image_path", currentPhotoPath);
            intent.putExtra("food_items", foodItems);
            intent.putExtra("calories", calories);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting DietAnalysisActivity: " + e.getMessage(), e);
            Toast.makeText(this, "启动饮食分析失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private double getBaseCalories(String foodName) {
        switch (foodName) {
            case "米饭": return 130;
            case "鸡胸肉": return 165;
            case "鸡蛋": return 155;
            case "西兰花": return 34;
            case "胡萝卜": return 41;
            case "面包": return 265;
            case "苹果": return 52;
            default: return 100;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted by user");
                dispatchTakePictureIntent();
            } else {
                Log.d(TAG, "Camera permission denied by user");
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();

                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle("相机权限")
                            .setMessage("拍照功能需要相机权限才能使用")
                            .setPositiveButton("确定", (dialog, which) -> {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                finish();
                            })
                            .show();
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CameraActivity destroyed");
        if (ocrHelper != null) {
            ocrHelper.release();
        }
    }

    private void showCameraNotFoundDialog() {
        new AlertDialog.Builder(this)
                .setTitle("无法使用相机")
                .setMessage("您的设备上没有找到可用的相机应用。\n\n" +
                        "请确保：\n" +
                        "1. 设备上已安装相机应用\n" +
                        "2. 相机应用没有被禁用\n" +
                        "3. 系统相机应用工作正常")
                .setPositiveButton("确定", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}