package com.oppo.AIHealth.data;

import androidx.room.TypeConverters;

import com.oppo.AIHealth.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊断单结构化数据模型
 * 用于存储从OCR文本中解析出的关键信息
 */
public class DiagnosisStructured {
    private String diagnosis;          // 诊断结论
    private String advice;              // 医嘱
    private String allergy;             // 过敏提示
    private List<KeyIndicator> keyIndicators; // 关键指标列表

    public DiagnosisStructured() {
        this.diagnosis = "";
        this.advice = "";
        this.allergy = "";
        this.keyIndicators = new ArrayList<>();
    }

    // Getter和Setter
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public String getAllergy() { return allergy; }
    public void setAllergy(String allergy) { this.allergy = allergy; }

    public List<KeyIndicator> getKeyIndicators() { return keyIndicators; }
    public void setKeyIndicators(List<KeyIndicator> keyIndicators) { this.keyIndicators = keyIndicators; }

    /**
     * 关键指标内部类
     */
    public static class KeyIndicator {
        private String name;   // 指标名称，如"血压"
        private String value;  // 指标值，如"120/80"
        private String unit;   // 单位，如"mmHg"

        public KeyIndicator() {}

        public KeyIndicator(String name, String value, String unit) {
            this.name = name;
            this.value = value;
            this.unit = unit;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    /**
     * 获取诊断摘要（用于列表展示）
     */
    public String getSummary() {
        if (diagnosis != null && !diagnosis.isEmpty()) {
            return diagnosis.length() > 50 ? diagnosis.substring(0, 50) + "..." : diagnosis;
        } else if (advice != null && !advice.isEmpty()) {
            return advice.length() > 50 ? advice.substring(0, 50) + "..." : advice;
        } else {
            return "诊断单已保存";
        }
    }

    /**
     * 获取疾病类型（用于分类）
     */
    public String getDiseaseType() {
        if (diagnosis == null) return "其他";
        String lower = diagnosis.toLowerCase();
        if (lower.contains("高血压")) return "高血压";
        if (lower.contains("糖尿病")) return "糖尿病";
        if (lower.contains("心脏病") || lower.contains("冠心病")) return "心脏病";
        if (lower.contains("感染")) return "感染";
        return "其他";
    }

    /**
     * 判断是否为紧急情况（简单规则）
     */
    public boolean isUrgent() {
        if (diagnosis == null) return false;
        String lower = diagnosis.toLowerCase();
        return lower.contains("重度") || lower.contains("严重") || lower.contains("危急") ||
                lower.contains("酮症") || lower.contains("昏迷") || lower.contains("败血症");
    }
}