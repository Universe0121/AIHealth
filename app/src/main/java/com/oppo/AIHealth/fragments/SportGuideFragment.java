package com.oppo.AIHealth.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.oppo.AIHealth.R;

public class SportGuideFragment extends Fragment {

    private static final String TAG = "SportGuideFragment";
    private Spinner spSportType, spIntensity;
    private EditText etDuration, etGoal;
    private Button btnGenerateGuide;
    private TextView tvGuideResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sport_guide, container, false);
        initViews(view);
        setupSpinners();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        try {
            spSportType = view.findViewById(R.id.sp_sport_type);
            spIntensity = view.findViewById(R.id.sp_intensity);
            etDuration = view.findViewById(R.id.et_duration);
            etGoal = view.findViewById(R.id.et_goal);
            btnGenerateGuide = view.findViewById(R.id.btn_generate_guide);
            tvGuideResult = view.findViewById(R.id.tv_guide_result);

            // 检查所有控件是否成功初始化
            if (spSportType == null) Log.e(TAG, "spSportType is null");
            if (spIntensity == null) Log.e(TAG, "spIntensity is null");
            if (etDuration == null) Log.e(TAG, "etDuration is null");
            if (etGoal == null) Log.e(TAG, "etGoal is null");
            if (btnGenerateGuide == null) Log.e(TAG, "btnGenerateGuide is null");
            if (tvGuideResult == null) Log.e(TAG, "tvGuideResult is null");

            Log.d(TAG, "所有控件初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化控件失败: " + e.getMessage());
            Toast.makeText(getContext(), "页面加载失败，请稍后再试", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinners() {
        try {
            // 设置运动类型下拉列表
            ArrayAdapter<CharSequence> sportTypeAdapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.sport_types,
                    android.R.layout.simple_spinner_item
            );
            sportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spSportType.setAdapter(sportTypeAdapter);

            // 设置运动强度下拉列表
            ArrayAdapter<CharSequence> intensityAdapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.intensity_levels,
                    android.R.layout.simple_spinner_item
            );
            intensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spIntensity.setAdapter(intensityAdapter);

            Log.d(TAG, "下拉列表设置完成");
        } catch (Exception e) {
            Log.e(TAG, "设置下拉列表失败: " + e.getMessage());
            Toast.makeText(getContext(), "数据加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        if (btnGenerateGuide != null) {
            btnGenerateGuide.setOnClickListener(v -> {
                Log.d(TAG, "生成按钮被点击");
                generateSportGuide();
            });
        } else {
            Log.e(TAG, "生成按钮为空，无法设置点击事件");
        }
    }

    private void generateSportGuide() {
        try {
            // 1. 获取用户输入
            String sportType = (spSportType != null && spSportType.getSelectedItem() != null)
                    ? spSportType.getSelectedItem().toString() : "跑步";
            String intensity = (spIntensity != null && spIntensity.getSelectedItem() != null)
                    ? spIntensity.getSelectedItem().toString() : "中等强度";
            String duration = (etDuration != null) ? etDuration.getText().toString().trim() : "";
            String goal = (etGoal != null) ? etGoal.getText().toString().trim() : "";

            Log.d(TAG, "用户输入：运动类型=" + sportType + ", 强度=" + intensity +
                    ", 时长=" + duration + ", 目标=" + goal);

            // 2. 输入校验
            if (duration.isEmpty()) {
                Toast.makeText(requireContext(), "⚠️ 请输入运动时长（如30）", Toast.LENGTH_LONG).show();
                if (etDuration != null) {
                    etDuration.requestFocus();
                }
                return;
            }

            if (goal.isEmpty()) {
                Toast.makeText(requireContext(), "⚠️ 请输入运动目标（如减脂）", Toast.LENGTH_LONG).show();
                if (etGoal != null) {
                    etGoal.requestFocus();
                }
                return;
            }

            // 3. 生成指导建议
            String guide = String.format(
                    "【个性化运动指导】\n" + "运动类型：%s\n" + "运动强度：%s\n" + "运动时长：%s分钟\n" + "运动目标：%s\n\n" + " 指导建议：\n" +
                            "1. 运动前热身5-10分钟，并监测血糖，确保血糖在5.6-13.9 mmol/L之间，避免在血糖过高或过低时运动。\n" +
                            "2. 保持中等强度（如慢跑），心率控制在（220-年龄）×60%%~70%%范围，运动过程中如有不适立即停止。\n" +
                            "3. 运动后及时拉伸，补充水分和电解质，并再次监测血糖，预防延迟性低血糖。\n" +
                            "4. 每周坚持3-5次，每次30-45分钟效果更佳，可根据体能逐步增加时长。\n" +
                            "5. 根据身体反应适当调整运动强度，注意随身携带糖果或零食，以防低血糖发生。",
                    sportType, intensity, duration, goal
            );

            Log.d(TAG, "指导建议生成完成");

            // 4. 显示结果
            if (tvGuideResult != null) {
                tvGuideResult.setText(guide);
                tvGuideResult.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "✅ 个性化指导已生成", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "❌ 结果展示控件加载失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "生成运动指导失败: " + e.getMessage());
            Toast.makeText(requireContext(), "生成指导时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}