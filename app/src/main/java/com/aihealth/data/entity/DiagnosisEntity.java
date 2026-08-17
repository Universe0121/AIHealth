package com.aihealth.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aihealth.data.db.Converters;
import com.aihealth.data.model.DiagnosisStructured;

import java.util.Date;

/**
 * 诊断单记录实体类（Room数据库）
 */
@Entity(tableName = "diagnosis")
@TypeConverters(Converters.class)
public class DiagnosisEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;                     // 主键
    private String ocrText;              // 原始OCR文本
    private DiagnosisStructured structuredData; // 结构化数据（将自动转换为JSON存储）
    private String imagePath;            // 图片本地路径
    private long imageSize;              // 图片大小（字节）
    private String imageDate;            // 图片拍摄日期（如"2025-03-10"）
    private Date timestamp;              // 记录创建时间

    // 构造函数
    public DiagnosisEntity() {}

    // Getter和Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }

    public DiagnosisStructured getStructuredData() { return structuredData; }
    public void setStructuredData(DiagnosisStructured structuredData) { this.structuredData = structuredData; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public long getImageSize() { return imageSize; }
    public void setImageSize(long imageSize) { this.imageSize = imageSize; }

    public String getImageDate() { return imageDate; }
    public void setImageDate(String imageDate) { this.imageDate = imageDate; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}