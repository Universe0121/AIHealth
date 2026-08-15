package com.oppo.AIHealth.utils;

import android.text.TextUtils;
import android.util.Log;

import com.oppo.AIHealth.data.DiagnosisStructured;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 诊断单OCR文本解析器
 * 从非结构化文本中提取诊断结论、医嘱、过敏提示及关键指标
 */
public class DiagnosisParser {
    private static final String TAG = "DiagnosisParser";

    // 常见指标名称及其单位
    private static final String[] INDICATOR_PATTERNS = {
            "血压\\s*[:：]?\\s*(\\d{2,3})/(\\d{2,3})\\s*(mmHg)?",
            "血糖\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(mmol/L)?",
            "心率\\s*[:：]?\\s*(\\d{2,3})\\s*(次/分)?",
            "血脂\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(mmol/L)?",
            "总胆固醇\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(mmol/L)?",
            "甘油三酯\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(mmol/L)?",
            "体重\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(kg)?",
            "身高\\s*[:：]?\\s*(\\d+\\.?\\d*)\\s*(cm)?",
            "BMI\\s*[:：]?\\s*(\\d+\\.?\\d*)"
    };

    /**
     * 解析诊断单OCR文本，返回结构化对象
     */
    public static DiagnosisStructured parse(String ocrText) {
        if (TextUtils.isEmpty(ocrText)) {
            return new DiagnosisStructured();
        }

        DiagnosisStructured structured = new DiagnosisStructured();
        String[] lines = ocrText.split("\n");
        List<String> validLines = new ArrayList<>();

        // 清洗文本：去除空行和无效符号
        for (String line : lines) {
            String trimLine = line.trim();
            if (!trimLine.isEmpty() && !trimLine.matches("[\\[\\]【】()（）]+")) {
                validLines.add(trimLine);
            }
        }

        // 提取诊断结论、医嘱、过敏提示
        extractDiagnosisInfo(validLines, structured);
        // 提取关键指标
        extractKeyIndicators(ocrText, structured);

        return structured;
    }

    /**
     * 提取诊断结论、医嘱、过敏提示
     */
    private static void extractDiagnosisInfo(List<String> lines, DiagnosisStructured structured) {
        StringBuilder diagnosisBuilder = new StringBuilder();
        StringBuilder adviceBuilder = new StringBuilder();
        StringBuilder allergyBuilder = new StringBuilder();

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            // 诊断结论（包含“诊断”、“结论”等关键词）
            if (lowerLine.contains("诊断") || lowerLine.contains("结论") ||
                    lowerLine.contains("印象") || lowerLine.contains("初步诊断")) {
                // 尝试提取冒号后的内容
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    diagnosisBuilder.append(content).append("\n");
                } else {
                    diagnosisBuilder.append(line).append("\n");
                }
            }
            // 医嘱（包含“医嘱”、“建议”、“嘱”等关键词）
            else if (lowerLine.contains("医嘱") || lowerLine.contains("建议") ||
                    lowerLine.contains("嘱") || lowerLine.contains("指导")) {
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    adviceBuilder.append(content).append("\n");
                } else {
                    adviceBuilder.append(line).append("\n");
                }
            }
            // 过敏提示（包含“过敏”、“禁忌”等关键词）
            else if (lowerLine.contains("过敏") || lowerLine.contains("禁忌") ||
                    lowerLine.contains("禁用") || lowerLine.contains("慎用")) {
                String content = extractAfterColon(line);
                if (!content.isEmpty()) {
                    allergyBuilder.append(content).append("\n");
                } else {
                    allergyBuilder.append(line).append("\n");
                }
            }
        }

        structured.setDiagnosis(diagnosisBuilder.toString().trim());
        structured.setAdvice(adviceBuilder.toString().trim());
        structured.setAllergy(allergyBuilder.toString().trim());
    }

    /**
     * 提取冒号后的内容（去除冒号前的部分）
     */
    private static String extractAfterColon(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) colonIndex = line.indexOf("：");
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }

    /**
     * 提取关键指标（血压、血糖等）
     */
    private static void extractKeyIndicators(String text, DiagnosisStructured structured) {
        List<DiagnosisStructured.KeyIndicator> indicators = new ArrayList<>();

        for (String patternStr : INDICATOR_PATTERNS) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String indicatorName = getIndicatorName(patternStr);
                String value = matcher.group(1);
                String unit = matcher.groupCount() >= 2 ? matcher.group(2) : "";

                // 特殊处理血压（有多个数值）
                if (indicatorName.equals("血压") && matcher.groupCount() >= 2) {
                    value = matcher.group(1) + "/" + matcher.group(2);
                    unit = matcher.groupCount() >= 3 ? matcher.group(3) : "mmHg";
                }

                // 单位清理
                if (unit != null && unit.contains("(") || unit.contains("（")) {
                    unit = unit.replaceAll("[（(]", "").replaceAll("[）)]", "");
                }

                DiagnosisStructured.KeyIndicator indicator =
                        new DiagnosisStructured.KeyIndicator(indicatorName, value, unit);
                indicators.add(indicator);
            }
        }

        structured.setKeyIndicators(indicators);
    }

    /**
     * 从正则表达式中提取指标名称
     */
    private static String getIndicatorName(String patternStr) {
        if (patternStr.startsWith("血压")) return "血压";
        if (patternStr.startsWith("血糖")) return "血糖";
        if (patternStr.startsWith("心率")) return "心率";
        if (patternStr.startsWith("血脂")) return "血脂";
        if (patternStr.startsWith("总胆固醇")) return "总胆固醇";
        if (patternStr.startsWith("甘油三酯")) return "甘油三酯";
        if (patternStr.startsWith("体重")) return "体重";
        if (patternStr.startsWith("身高")) return "身高";
        if (patternStr.startsWith("BMI")) return "BMI";
        return "其他";
    }
}