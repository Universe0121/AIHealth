package com.aihealth.ui.model;

import java.util.Date;

public class HistoryItem {
    private int id;
    private String ocrResult;
    private String imagePath;
    private long imageSize;
    private String imageDate;
    private Date timestamp;
    private String structuredJson; // 新增：结构化数据 JSON
    private String diseaseType; // 疾病类型
    private boolean isUrgent;   // 是否紧急

    // 构造函数（新增 structuredJson 参数）
    public HistoryItem(int id, String ocrResult, String imagePath,
                       long imageSize, String imageDate, Date timestamp,
                       String structuredJson) {
        this.id = id;
        this.ocrResult = ocrResult;
        this.imagePath = imagePath;
        this.imageSize = imageSize;
        this.imageDate = imageDate;
        this.timestamp = timestamp;
        this.structuredJson = structuredJson;
        extractDiseaseInfo();
    }

    // 从 OCR 结果中提取疾病信息
    private void extractDiseaseInfo() {
        if (ocrResult.contains("高血压")) {
            diseaseType = "高血压";
            isUrgent = ocrResult.contains("重度") ||
                    ocrResult.contains("严重") ||
                    ocrResult.contains("危急");
        } else if (ocrResult.contains("糖尿病")) {
            diseaseType = "糖尿病";
            isUrgent = ocrResult.contains("重度") ||
                    ocrResult.contains("酮症") ||
                    ocrResult.contains("昏迷");
        } else if (ocrResult.contains("心脏病")) {
            diseaseType = "心脏病";
            isUrgent = true;
        } else if (ocrResult.contains("感染")) {
            diseaseType = "感染";
            isUrgent = ocrResult.contains("严重") ||
                    ocrResult.contains("败血症");
        } else {
            diseaseType = "其他";
            isUrgent = false;
        }
    }

    // Getter和Setter方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOcrResult() { return ocrResult; }
    public void setOcrResult(String ocrResult) {
        this.ocrResult = ocrResult;
        extractDiseaseInfo();
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public long getImageSize() { return imageSize; }
    public void setImageSize(long imageSize) { this.imageSize = imageSize; }

    public String getImageDate() { return imageDate; }
    public void setImageDate(String imageDate) { this.imageDate = imageDate; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getStructuredJson() { return structuredJson; }
    public void setStructuredJson(String structuredJson) { this.structuredJson = structuredJson; }

    public String getDiseaseType() { return diseaseType; }
    public boolean isUrgent() { return isUrgent; }

    // 获取摘要（前100个字符）
    public String getSummary() {
        if (ocrResult == null) return "";
        if (ocrResult.length() <= 100) return ocrResult;
        return ocrResult.substring(0, 100) + "...";
    }

    // 获取格式化的图片信息
    public String getFormattedImageInfo() {
        String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
        return fileName + " · " + String.format("%.1f KB", imageSize / 1024.0);
    }

    // 获取格式化的日期时间
    public String getFormattedDateTime() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault());
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat(
                "HH:mm:ss", java.util.Locale.getDefault());

        return dateFormat.format(timestamp) + "\n" + timeFormat.format(timestamp);
    }

    // 获取日期部分
    public String getDatePart() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault());
        return dateFormat.format(timestamp);
    }

    // 获取时间部分
    public String getTimePart() {
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat(
                "HH:mm:ss", java.util.Locale.getDefault());
        return timeFormat.format(timestamp);
    }
}