package com.aihealth.util;

import android.text.TextUtils;

import com.aihealth.data.model.DiagnosisStructured;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊断单 OCR 文本解析器。
 *
 * <p>从非结构化的 OCR 文本中提取诊断结论、医嘱、过敏提示及血压/血糖等关键指标，
 * 并提供缺失字段检测能力，供多图拍摄补全流程使用。</p>
 */
public final class OcrDiagnosisParser {

    private OcrDiagnosisParser() {
    }

    /** 诊断单解析结果（含缺失字段检测） */
    public static class Result {
        public String diagnosis = "";
        public String advice = "";
        public String allergy = "";
        public String bloodPressure = "";
        public String bloodSugar = "";

        public boolean isComplete() {
            return !TextUtils.isEmpty(diagnosis) && !TextUtils.isEmpty(advice);
        }

        public List<String> getMissingFields() {
            List<String> missing = new ArrayList<>();
            if (TextUtils.isEmpty(diagnosis)) missing.add("诊断结论");
            if (TextUtils.isEmpty(advice)) missing.add("医嘱");
            return missing;
        }

        public DiagnosisStructured toStructured() {
            DiagnosisStructured structured = new DiagnosisStructured();
            structured.setDiagnosis(diagnosis);
            structured.setAdvice(advice);
            structured.setAllergy(allergy);

            List<DiagnosisStructured.KeyIndicator> indicators = new ArrayList<>();
            if (!TextUtils.isEmpty(bloodPressure)) {
                String value = bloodPressure;
                String unit = "";
                int spaceIdx = bloodPressure.lastIndexOf(" ");
                if (spaceIdx != -1) {
                    value = bloodPressure.substring(0, spaceIdx);
                    unit = bloodPressure.substring(spaceIdx + 1);
                }
                indicators.add(new DiagnosisStructured.KeyIndicator("血压", value, unit));
            }
            if (!TextUtils.isEmpty(bloodSugar)) {
                String value = bloodSugar;
                String unit = "";
                int spaceIdx = bloodSugar.lastIndexOf(" ");
                if (spaceIdx != -1) {
                    value = bloodSugar.substring(0, spaceIdx);
                    unit = bloodSugar.substring(spaceIdx + 1);
                }
                indicators.add(new DiagnosisStructured.KeyIndicator("血糖", value, unit));
            }
            structured.setKeyIndicators(indicators);
            return structured;
        }
    }

    /** 合并多张图片的 OCR 识别结果 */
    public static String mergeResults(List<String> results) {
        StringBuilder sb = new StringBuilder();
        for (String result : results) {
            if (!TextUtils.isEmpty(result)) {
                sb.append(result).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** 解析诊断单 OCR 文本，返回结构化结果 */
    public static Result parse(String ocrText) {
        Result result = new Result();
        if (TextUtils.isEmpty(ocrText)) {
            return result;
        }

        String[] lines = ocrText.split("\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            String trimLine = line.trim();
            if (!trimLine.isEmpty() && !trimLine.matches("[\\[\\]【】()（）]+")) {
                validLines.add(trimLine);
            }
        }

        final String KEY_DIAGNOSIS = "诊断|结论|印象|拟诊";
        final String KEY_ADVICE = "医嘱|建议|嘱";
        final String KEY_ALLERGY = "过敏|禁忌|禁用|慎用";

        StringBuilder diagnosisBuilder = new StringBuilder();
        StringBuilder adviceBuilder = new StringBuilder();
        StringBuilder allergyBuilder = new StringBuilder();

        String currentSection = "";

        for (int i = 0; i < validLines.size(); i++) {
            String line = validLines.get(i);
            String lowerLine = line.toLowerCase();

            if (lowerLine.matches(".*(" + KEY_DIAGNOSIS + ").*")) {
                currentSection = "diagnosis";
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    diagnosisBuilder.append(content).append("\n");
                } else {
                    String titleRemoved = line.replaceAll("(?i)(" + KEY_DIAGNOSIS + ")", "").trim();
                    if (!titleRemoved.isEmpty()) {
                        diagnosisBuilder.append(titleRemoved).append("\n");
                    }
                }
                continue;
            }

            if (lowerLine.matches(".*(" + KEY_ADVICE + ").*")) {
                currentSection = "advice";
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    adviceBuilder.append(content).append("\n");
                } else {
                    String titleRemoved = line.replaceAll("(?i)(" + KEY_ADVICE + ")", "").trim();
                    if (!titleRemoved.isEmpty()) {
                        adviceBuilder.append(titleRemoved).append("\n");
                    }
                }
                continue;
            }

            if (lowerLine.matches(".*(" + KEY_ALLERGY + ").*")) {
                currentSection = "allergy";
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    allergyBuilder.append(content).append("\n");
                } else {
                    String titleRemoved = line.replaceAll("(?i)(" + KEY_ALLERGY + ")", "").trim();
                    if (!titleRemoved.isEmpty()) {
                        allergyBuilder.append(titleRemoved).append("\n");
                    }
                }
                continue;
            }

            if (lowerLine.contains("血压") || lowerLine.contains("血糖") ||
                    lowerLine.contains("心率") || lowerLine.contains("体温")) {
                // 指标行不改变章节
            }

            if (lowerLine.contains("【规格】") || lowerLine.contains("规格") ||
                    lowerLine.contains("【不良反应】") || lowerLine.contains("不良反应") ||
                    lowerLine.contains("【禁忌】") || lowerLine.contains("禁忌") ||
                    lowerLine.contains("【注意事项】") || lowerLine.contains("注意事项") ||
                    lowerLine.contains("【药理毒理】") || lowerLine.contains("药理毒理") ||
                    lowerLine.contains("【贮藏】") || lowerLine.contains("贮藏") ||
                    lowerLine.contains("【包装】") || lowerLine.contains("包装") ||
                    lowerLine.contains("【有效期】") || lowerLine.contains("有效期") ||
                    lowerLine.contains("【执行标准】") || lowerLine.contains("执行标准") ||
                    lowerLine.contains("【批准文号】") || lowerLine.contains("批准文号") ||
                    lowerLine.contains("【生产企业】") || lowerLine.contains("生产企业")) {
                currentSection = "";
                continue;
            }

            if (!currentSection.isEmpty()) {
                switch (currentSection) {
                    case "diagnosis":
                        diagnosisBuilder.append(line).append("\n");
                        break;
                    case "advice":
                        adviceBuilder.append(line).append("\n");
                        break;
                    case "allergy":
                        allergyBuilder.append(line).append("\n");
                        break;
                }
            }
        }

        if (diagnosisBuilder.length() == 0 && !validLines.isEmpty()) {
            diagnosisBuilder.append(validLines.get(0)).append("\n");
        }

        String bloodPressure = extractIndicator(validLines, "血压");
        String bloodSugar = extractIndicator(validLines, "血糖");

        result.diagnosis = diagnosisBuilder.toString().trim();
        result.advice = adviceBuilder.toString().trim();
        result.allergy = allergyBuilder.toString().trim();
        result.bloodPressure = bloodPressure;
        result.bloodSugar = bloodSugar;

        return result;
    }

    private static String extractIndicator(List<String> lines, String indicatorName) {
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains(indicatorName.toLowerCase())) {
                String value = extractAfterColon(line);
                if (!value.isEmpty()) {
                    return value;
                } else {
                    return line.replaceAll("(?i)" + indicatorName, "").trim();
                }
            }
        }
        return "";
    }

    private static String extractAfterColon(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) colonIndex = line.indexOf("：");
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }
}
