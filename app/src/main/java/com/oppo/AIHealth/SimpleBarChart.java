package com.oppo.AIHealth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SimpleBarChart extends View {
    private List<Float> dataPoints = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Paint barPaint, gridPaint, textPaint;
    private String chartTitle = "血糖趋势图";
    private String yAxisLabel = "mmol/L";
    private float maxValue = 10f;
    private float minValue = 3f;
    private int normalColor = Color.parseColor("#4CAF50"); // 正常范围：绿色
    private int warningColor = Color.parseColor("#FF9800"); // 警告范围：橙色
    private int dangerColor = Color.parseColor("#F44336"); // 危险范围：红色
    private float normalThreshold = 6.1f; // 正常上限
    private float warningThreshold = 7.8f; // 警告上限

    public SimpleBarChart(Context context) {
        super(context);
        init();
    }

    public SimpleBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 柱状图画笔
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        // 网格画笔
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(spToPx(12));

        // 添加示例数据
        addSampleData();
    }

    // 添加示例数据
    private void addSampleData() {
        // 模拟血糖数据（空腹血糖）
        dataPoints.add(7.8f);
        dataPoints.add(7.2f);
        dataPoints.add(6.9f);
        dataPoints.add(6.5f);
        dataPoints.add(7.1f);
        dataPoints.add(6.8f);
        dataPoints.add(6.3f);

        // 标签
        labels.add("周一");
        labels.add("周二");
        labels.add("周三");
        labels.add("周四");
        labels.add("周五");
        labels.add("周六");
        labels.add("周日");
    }

    // 设置数据
    public void setData(List<Float> dataPoints, List<String> labels) {
        this.dataPoints = dataPoints;
        this.labels = labels;
        calculateMinMax();
        invalidate();
    }

    // 添加单个数据点
    public void addDataPoint(float value, String label) {
        dataPoints.add(value);
        labels.add(label);

        // 保持最多10个数据点
        if (dataPoints.size() > 10) {
            dataPoints.remove(0);
            labels.remove(0);
        }

        calculateMinMax();
        invalidate();
    }

    // 计算最大值最小值
    private void calculateMinMax() {
        if (dataPoints.isEmpty()) return;

        maxValue = dataPoints.get(0);
        minValue = dataPoints.get(0);

        for (Float value : dataPoints) {
            if (value > maxValue) maxValue = value;
            if (value < minValue) minValue = value;
        }

        // 添加一些边距
        float range = maxValue - minValue;
        maxValue += range * 0.2f;
        minValue = Math.min(minValue, 0f); // 最小值至少为0

        if (maxValue - minValue < 1) {
            maxValue += 1;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.isEmpty()) {
            drawEmptyChart(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // 计算边距
        int leftMargin = dpToPx(50);
        int rightMargin = dpToPx(20);
        int topMargin = dpToPx(40);
        int bottomMargin = dpToPx(50);

        int chartWidth = width - leftMargin - rightMargin;
        int chartHeight = height - topMargin - bottomMargin;

        // 绘制标题
        drawTitle(canvas, width, topMargin);

        // 绘制参考线
        drawReferenceLines(canvas, leftMargin, topMargin, chartWidth, chartHeight);

        // 绘制Y轴标签
        drawYAxisLabels(canvas, leftMargin, topMargin, chartHeight);

        // 绘制X轴标签
        drawXAxisLabels(canvas, leftMargin, topMargin, chartWidth, chartHeight, bottomMargin);

        // 绘制柱状图
        drawBars(canvas, leftMargin, topMargin, chartWidth, chartHeight);
    }

    // 绘制空图表
    private void drawEmptyChart(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(spToPx(16));
        textPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("暂无血糖数据", width / 2, height / 2, textPaint);
    }

    // 绘制标题
    private void drawTitle(Canvas canvas, int width, int topMargin) {
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(spToPx(16));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);

        canvas.drawText(chartTitle, width / 2, topMargin / 2, titlePaint);
    }

    // 绘制参考线（正常、警告范围）
    private void drawReferenceLines(Canvas canvas, int left, int top, int width, int height) {
        // 正常范围上限线
        float normalY = top + height - ((normalThreshold - minValue) / (maxValue - minValue)) * height;
        Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalPaint.setColor(normalColor);
        normalPaint.setStyle(Paint.Style.STROKE);
        normalPaint.setStrokeWidth(2f);
        normalPaint.setAlpha(150);
        canvas.drawLine(left, normalY, left + width, normalY, normalPaint);

        // 警告范围上限线
        float warningY = top + height - ((warningThreshold - minValue) / (maxValue - minValue)) * height;
        Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        warningPaint.setColor(warningColor);
        warningPaint.setStyle(Paint.Style.STROKE);
        warningPaint.setStrokeWidth(2f);
        warningPaint.setAlpha(150);
        canvas.drawLine(left, warningY, left + width, warningY, warningPaint);

        // 添加图例
        drawLegend(canvas, left + width - dpToPx(100), top + dpToPx(10));
    }

    // 绘制图例
    private void drawLegend(Canvas canvas, float x, float y) {
        Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legendPaint.setTextSize(spToPx(10));

        // 正常范围
        legendPaint.setColor(normalColor);
        canvas.drawRect(x, y, x + dpToPx(10), y + dpToPx(10), legendPaint);
        canvas.drawText("正常(<" + normalThreshold + ")", x + dpToPx(15), y + dpToPx(9), legendPaint);

        // 警告范围
        legendPaint.setColor(warningColor);
        canvas.drawRect(x, y + dpToPx(15), x + dpToPx(10), y + dpToPx(25), legendPaint);
        canvas.drawText("警告(<" + warningThreshold + ")", x + dpToPx(15), y + dpToPx(24), legendPaint);

        // 危险范围
        legendPaint.setColor(dangerColor);
        canvas.drawRect(x, y + dpToPx(30), x + dpToPx(10), y + dpToPx(40), legendPaint);
        canvas.drawText("危险(≥" + warningThreshold + ")", x + dpToPx(15), y + dpToPx(39), legendPaint);
    }

    // 绘制Y轴标签
    private void drawYAxisLabels(Canvas canvas, int left, int top, int height) {
        Paint yLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPaint.setColor(Color.BLACK);
        yLabelPaint.setTextSize(spToPx(10));
        yLabelPaint.setTextAlign(Paint.Align.RIGHT);

        // 绘制4个Y轴标签
        int numLabels = 4;
        for (int i = 0; i <= numLabels; i++) {
            float value = maxValue - (maxValue - minValue) * i / numLabels;
            float y = top + (height * i / numLabels);

            DecimalFormat df = new DecimalFormat("#.#");
            String label = df.format(value);

            canvas.drawText(label, left - dpToPx(5), y + spToPx(4), yLabelPaint);
        }

        // 绘制Y轴单位
        yLabelPaint.setTextSize(spToPx(12));
        yLabelPaint.setTextAlign(Paint.Align.CENTER);

        canvas.save();
        canvas.rotate(-90, left - dpToPx(30), top + height / 2);
        canvas.drawText(yAxisLabel, left - dpToPx(30), top + height / 2, yLabelPaint);
        canvas.restore();
    }

    // 绘制X轴标签
    private void drawXAxisLabels(Canvas canvas, int left, int top, int width, int height, int bottomMargin) {
        Paint xLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xLabelPaint.setColor(Color.BLACK);
        xLabelPaint.setTextSize(spToPx(10));
        xLabelPaint.setTextAlign(Paint.Align.CENTER);

        float barWidth = width / (dataPoints.size() * 1.5f);
        float spacing = barWidth * 0.5f;

        for (int i = 0; i < dataPoints.size(); i++) {
            float x = left + spacing + (barWidth + spacing) * i + barWidth / 2;
            float y = top + height + bottomMargin / 3;

            String label = labels.get(i);
            canvas.drawText(label, x, y, xLabelPaint);
        }
    }

    // 绘制柱状图
    private void drawBars(Canvas canvas, int left, int top, int width, int height) {
        float barWidth = width / (dataPoints.size() * 1.5f);
        float spacing = barWidth * 0.5f;

        for (int i = 0; i < dataPoints.size(); i++) {
            float value = dataPoints.get(i);
            float barHeight = ((value - minValue) / (maxValue - minValue)) * height;
            float x = left + spacing + (barWidth + spacing) * i;
            float y = top + height - barHeight;

            // 根据数值选择颜色
            if (value < normalThreshold) {
                barPaint.setColor(normalColor);
            } else if (value < warningThreshold) {
                barPaint.setColor(warningColor);
            } else {
                barPaint.setColor(dangerColor);
            }

            // 绘制柱状图
            RectF rect = new RectF(x, y, x + barWidth, top + height);
            canvas.drawRoundRect(rect, dpToPx(2), dpToPx(2), barPaint);

            // 绘制数值
            DecimalFormat df = new DecimalFormat("#.#");
            String valueText = df.format(value);

            Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            valuePaint.setColor(Color.BLACK);
            valuePaint.setTextSize(spToPx(10));
            valuePaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(valueText, x + barWidth / 2, y - dpToPx(5), valuePaint);
        }
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