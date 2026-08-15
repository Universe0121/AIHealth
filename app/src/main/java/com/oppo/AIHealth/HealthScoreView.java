package com.oppo.AIHealth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class HealthScoreView extends View {
    private int score = 75; // 默认75分
    private Paint circlePaint, textPaint, backgroundPaint;
    private String title = "健康评分";
    private String description = "根据您的健康数据计算";

    // 评分等级颜色
    private int excellentColor = Color.parseColor("#4CAF50"); // 优秀：90-100
    private int goodColor = Color.parseColor("#8BC34A");      // 良好：80-89
    private int mediumColor = Color.parseColor("#FFC107");    // 中等：70-79
    private int poorColor = Color.parseColor("#FF9800");      // 较差：60-69
    private int badColor = Color.parseColor("#F44336");       // 差：0-59

    public HealthScoreView(Context context) {
        super(context);
        init();
    }

    public HealthScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 背景圆环画笔
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#E0E0E0"));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(dpToPx(10));

        // 进度圆环画笔
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(dpToPx(10));
        circlePaint.setStrokeCap(Paint.Cap.ROUND);

        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
    }

    // 获取分数
    public int getScore() {
        return score;
    }

    // 设置分数
    public void setScore(int score) {
        this.score = Math.max(0, Math.min(100, score));
        invalidate();
    }

    // 设置标题
    public void setTitle(String title) {
        this.title = title;
        invalidate();
    }

    // 设置描述
    public void setDescription(String description) {
        this.description = description;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // 计算圆环半径（留出边距）
        int radius = Math.min(width, height) / 2 - dpToPx(20);

        // 绘制背景圆环
        RectF oval = new RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );
        canvas.drawArc(oval, 0, 360, false, backgroundPaint);

        // 根据分数设置颜色
        circlePaint.setColor(getScoreColor(score));

        // 计算进度角度（360度对应100分）
        float sweepAngle = 360 * score / 100f;

        // 从顶部开始绘制（-90度偏移）
        canvas.drawArc(oval, -90, sweepAngle, false, circlePaint);

        // 绘制分数
        textPaint.setTextSize(spToPx(28));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(score), centerX, centerY + spToPx(10), textPaint);

        // 绘制"分"字
        textPaint.setTextSize(spToPx(14));
        textPaint.setFakeBoldText(false);
        canvas.drawText("分", centerX + dpToPx(20), centerY, textPaint);

        // 绘制标题
        textPaint.setTextSize(spToPx(16));
        textPaint.setColor(Color.BLACK);
        canvas.drawText(title, centerX, centerY - radius - dpToPx(10), textPaint);

        // 绘制描述
        textPaint.setTextSize(spToPx(12));
        textPaint.setColor(Color.GRAY);
        canvas.drawText(description, centerX, centerY + radius + dpToPx(20), textPaint);

        // 绘制评分等级
        drawScoreLevel(canvas, centerX, centerY + radius + dpToPx(40));
    }

    // 根据分数获取颜色
    private int getScoreColor(int score) {
        if (score >= 90) return excellentColor;
        if (score >= 80) return goodColor;
        if (score >= 70) return mediumColor;
        if (score >= 60) return poorColor;
        return badColor;
    }

    // 获取评分等级文字
    public String getScoreLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "中等";
        if (score >= 60) return "较差";
        return "差";
    }

    // 绘制评分等级
    private void drawScoreLevel(Canvas canvas, int centerX, int y) {
        Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setTextSize(spToPx(14));
        levelPaint.setTextAlign(Paint.Align.CENTER);
        levelPaint.setColor(getScoreColor(score));
        levelPaint.setFakeBoldText(true);

        String levelText = getScoreLevel(score);
        canvas.drawText(levelText, centerX, y, levelPaint);
    }

    // 获取评分建议
    public String getScoreAdvice() {
        if (score >= 90) return "您的健康状况优秀，请继续保持良好的生活习惯！";
        if (score >= 80) return "您的健康状况良好，有少量改进空间。";
        if (score >= 70) return "您的健康状况中等，建议加强健康管理。";
        if (score >= 60) return "您的健康状况较差，需要重点关注并改善。";
        return "您的健康状况需要立即关注，建议咨询医生。";
    }

    // 获取详细评分分析
    public String getScoreAnalysis() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("健康评分分析：\n\n");

        if (score >= 90) {
            analysis.append("✅ 血压控制良好\n");
            analysis.append("✅ 血糖水平正常\n");
            analysis.append("✅ 用药依从性高\n");
            analysis.append("✅ 定期检查记录完整\n");
        } else if (score >= 80) {
            analysis.append("✅ 血压基本正常\n");
            analysis.append("✅ 血糖略偏高\n");
            analysis.append("⚠️ 偶尔漏服药物\n");
            analysis.append("✅ 检查记录完整\n");
        } else if (score >= 70) {
            analysis.append("⚠️ 血压偏高需关注\n");
            analysis.append("⚠️ 血糖控制不理想\n");
            analysis.append("⚠️ 用药依从性一般\n");
            analysis.append("✅ 有定期检查记录\n");
        } else if (score >= 60) {
            analysis.append("❌ 血压控制不佳\n");
            analysis.append("❌ 血糖水平偏高\n");
            analysis.append("⚠️ 经常漏服药物\n");
            analysis.append("⚠️ 检查记录不完整\n");
        } else {
            analysis.append("❌ 血压严重偏高\n");
            analysis.append("❌ 血糖控制极差\n");
            analysis.append("❌ 用药依从性差\n");
            analysis.append("❌ 缺乏定期检查\n");
        }

        analysis.append("\n建议：").append(getScoreAdvice());
        return analysis.toString();
    }

    // 工具方法：dp转px
    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // 工具方法：sp转px
    private int spToPx(float sp) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        return Math.round(sp * scaledDensity);
    }
}