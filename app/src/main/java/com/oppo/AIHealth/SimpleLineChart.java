package com.oppo.AIHealth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SimpleLineChart extends View {
    private List<Float> dataPoints = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Paint linePaint, pointPaint, gridPaint, textPaint;
    private String chartTitle = "血压趋势图";
    private String yAxisLabel = "mmHg";
    private float maxValue = 200f;
    private float minValue = 80f;
    private boolean showGrid = true;
    private int lineColor = Color.parseColor("#2196F3"); // 医疗蓝
    private int pointColor = Color.parseColor("#FF5722"); // 强调色

    public SimpleLineChart(Context context) {
        super(context);
        init();
    }

    public SimpleLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 折线画笔
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // 数据点画笔
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(pointColor);
        pointPaint.setStyle(Paint.Style.FILL);

        // 网格画笔
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(spToPx(12));

        // 添加示例数据
        addSampleData();
    }

    // 添加示例数据
    private void addSampleData() {
        // 模拟血压数据（收缩压）
        dataPoints.add(145f);
        dataPoints.add(142f);
        dataPoints.add(138f);
        dataPoints.add(135f);
        dataPoints.add(140f);
        dataPoints.add(136f);
        dataPoints.add(132f);

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
        invalidate(); // 重绘
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
        maxValue += range * 0.1f;
        minValue -= range * 0.1f;

        if (maxValue - minValue < 10) {
            maxValue += 10;
            minValue -= 10;
        }
    }

    // 设置图表标题
    public void setChartTitle(String title) {
        this.chartTitle = title;
        invalidate();
    }

    // 设置Y轴标签
    public void setYAxisLabel(String label) {
        this.yAxisLabel = label;
        invalidate();
    }

    // 设置线条颜色
    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.size() < 2) {
            drawEmptyChart(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // 计算边距
        int leftMargin = dpToPx(50);    // 左边距用于Y轴标签
        int rightMargin = dpToPx(20);   // 右边距
        int topMargin = dpToPx(40);     // 上边距用于标题
        int bottomMargin = dpToPx(40);  // 下边距用于X轴标签

        int chartWidth = width - leftMargin - rightMargin;
        int chartHeight = height - topMargin - bottomMargin;

        // 绘制标题
        drawTitle(canvas, width, topMargin);

        // 绘制网格
        if (showGrid) {
            drawGrid(canvas, leftMargin, topMargin, chartWidth, chartHeight);
        }

        // 绘制Y轴标签
        drawYAxisLabels(canvas, leftMargin, topMargin, chartHeight);

        // 绘制X轴标签
        drawXAxisLabels(canvas, leftMargin, topMargin, chartWidth, chartHeight, bottomMargin);

        // 绘制数据点和折线
        drawDataPoints(canvas, leftMargin, topMargin, chartWidth, chartHeight);
    }

    // 绘制空图表
    private void drawEmptyChart(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(spToPx(16));
        textPaint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText("暂无数据，请添加记录", width / 2, height / 2, textPaint);
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

    // 绘制网格
    private void drawGrid(Canvas canvas, int left, int top, int width, int height) {
        // 水平网格线
        int horizontalLines = 5;
        for (int i = 0; i <= horizontalLines; i++) {
            float y = top + (height * i / horizontalLines);
            canvas.drawLine(left, y, left + width, y, gridPaint);
        }

        // 垂直网格线（对应每个数据点）
        if (!dataPoints.isEmpty()) {
            int verticalLines = dataPoints.size() - 1;
            for (int i = 0; i <= verticalLines; i++) {
                float x = left + (width * i / verticalLines);
                canvas.drawLine(x, top, x, top + height, gridPaint);
            }
        }
    }

    // 绘制Y轴标签
    private void drawYAxisLabels(Canvas canvas, int left, int top, int height) {
        Paint yLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPaint.setColor(Color.BLACK);
        yLabelPaint.setTextSize(spToPx(10));
        yLabelPaint.setTextAlign(Paint.Align.RIGHT);

        // 绘制5个Y轴标签
        int numLabels = 5;
        for (int i = 0; i <= numLabels; i++) {
            float value = maxValue - (maxValue - minValue) * i / numLabels;
            float y = top + (height * i / numLabels);

            DecimalFormat df = new DecimalFormat("#.#");
            String label = df.format(value);

            // 标签位置稍微调整
            canvas.drawText(label, left - dpToPx(5), y + spToPx(4), yLabelPaint);
        }

        // 绘制Y轴单位
        yLabelPaint.setTextSize(spToPx(12));
        yLabelPaint.setTextAlign(Paint.Align.CENTER);

        // 旋转画布绘制垂直文字
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

        if (!dataPoints.isEmpty()) {
            for (int i = 0; i < dataPoints.size(); i++) {
                float x = left + (width * i / (dataPoints.size() - 1));
                float y = top + height + bottomMargin / 2;

                String label = labels.get(i);
                canvas.drawText(label, x, y, xLabelPaint);
            }
        }
    }

    // 绘制数据点和折线
    private void drawDataPoints(Canvas canvas, int left, int top, int width, int height) {
        if (dataPoints.size() < 2) return;

        Path path = new Path();
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#E3F2FD")); // 浅蓝色填充
        fillPaint.setStyle(Paint.Style.FILL);

        // 绘制填充区域路径
        Path fillPath = new Path();

        for (int i = 0; i < dataPoints.size(); i++) {
            float value = dataPoints.get(i);
            float x = left + (width * i / (dataPoints.size() - 1));
            float y = top + height - ((value - minValue) / (maxValue - minValue)) * height;

            // 绘制数据点
            canvas.drawCircle(x, y, dpToPx(4), pointPaint);

            // 绘制数据点上的数值
            DecimalFormat df = new DecimalFormat("#.#");
            String valueText = df.format(value);

            Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            valuePaint.setColor(Color.BLACK);
            valuePaint.setTextSize(spToPx(10));
            valuePaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(valueText, x, y - dpToPx(10), valuePaint);

            // 构建折线路径
            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, y + top + height);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }

            // 填充区域路径
            if (i == 0) {
                fillPath.moveTo(x, top + height);
            }
            if (i == dataPoints.size() - 1) {
                fillPath.lineTo(x, top + height);
                fillPath.close();
            }
        }

        // 绘制填充区域
        canvas.drawPath(fillPath, fillPaint);

        // 绘制折线
        canvas.drawPath(path, linePaint);
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

    // 获取最大值
    public float getMaxValue() {
        return maxValue;
    }

    // 获取最小值
    public float getMinValue() {
        return minValue;
    }
}