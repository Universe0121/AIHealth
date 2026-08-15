package com.oppo.AIHealth.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.oppo.AIHealth.R;
import com.oppo.AIHealth.CameraActivity;
import com.oppo.AIHealth.DietAnalysisActivity;

public class DietAnalysisFragment extends Fragment {

    // 新增：定义饮食分析拍照请求码
    private static final int REQUEST_CODE_DIET_CAMERA = 1003;

    private CardView cardDietAnalysis;
    private CardView cardHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet_analysis, container, false);
        initViews(view);
        setupAnimations();
        return view;
    }

    private void initViews(View view) {
        try {
            cardDietAnalysis = view.findViewById(R.id.card_diet_analysis);
            cardHistory = view.findViewById(R.id.card_history);

            if (cardDietAnalysis == null || cardHistory == null) {
                Toast.makeText(getActivity(), "布局加载失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 拍摄分析饮食按钮点击事件
            cardDietAnalysis.setOnClickListener(v -> {
                try {
                    animateCardClick(v, () -> {
                        try {
                            Intent intent = new Intent(getActivity(), CameraActivity.class);
                            // 明确标记来源为饮食分析
                            intent.putExtra("source", "diet_analysis");
                            // 核心修改1：使用startActivityForResult接收拍照返回结果
                            startActivityForResult(intent, REQUEST_CODE_DIET_CAMERA);
                            if (getActivity() != null) {
                                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "无法打开相机: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "点击事件错误", Toast.LENGTH_SHORT).show();
                }
            });

            // 查看历史记录按钮点击事件
            cardHistory.setOnClickListener(v -> {
                try {
                    animateCardClick(v, () -> {
                        try {
                            Intent intent = new Intent(getActivity(), DietAnalysisActivity.class);
                            intent.putExtra("show_history", true);
                            startActivity(intent);
                            if (getActivity() != null) {
                                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "无法打开历史记录: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "点击事件错误", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 核心修改2：新增onActivityResult方法接收拍照返回结果
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 处理饮食分析模块拍照返回结果
        if (requestCode == REQUEST_CODE_DIET_CAMERA) {
            // 使用系统常量，避免编译错误
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                // 获取饮食分析结果（根据CameraActivity返回的字段调整）
                String foodItems = data.getStringExtra("food_items");
                double calories = data.getDoubleExtra("calories", 0.0);

                // 跳转到饮食分析页面展示结果
                Intent intent = new Intent(getActivity(), DietAnalysisActivity.class);
                intent.putExtra("show_history", false);
                intent.putExtra("food_items", foodItems);
                intent.putExtra("calories", calories);
                startActivity(intent);

                Toast.makeText(getActivity(), "饮食识别成功", Toast.LENGTH_SHORT).show();
            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                // 用户取消拍照
                Toast.makeText(getActivity(), "已取消拍照", Toast.LENGTH_SHORT).show();
            } else {
                // 识别失败
                Toast.makeText(getActivity(), "饮食识别失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void animateCardClick(View view, Runnable action) {
        try {
            view.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(150)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            try {
                                view.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(150)
                                        .setInterpolator(new AccelerateDecelerateInterpolator())
                                        .setListener(null)
                                        .start();
                                action.run();
                            } catch (Exception e) {
                                e.printStackTrace();
                                action.run();
                            }
                        }
                    })
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
            action.run();
        }
    }

    private void setupAnimations() {
        try {
            if (cardDietAnalysis != null && cardHistory != null) {
                cardDietAnalysis.setAlpha(0f);
                cardHistory.setAlpha(0f);

                cardDietAnalysis.animate()
                        .alpha(1f)
                        .setStartDelay(200)
                        .setDuration(400)
                        .start();

                cardHistory.animate()
                        .alpha(1f)
                        .setStartDelay(300)
                        .setDuration(400)
                        .start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 忽略动画错误
        }
    }
}