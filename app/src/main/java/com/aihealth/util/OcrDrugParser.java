package com.aihealth.util;

import android.text.TextUtils;
import android.util.Log;

import com.aihealth.data.entity.Drug;

import java.util.ArrayList;
import java.util.List;

/**
 * 药品说明书 OCR 文本解析器。
 *
 * <p>从非结构化的 OCR 文本中提取药品名称、功能主治、用法用量、不良反应、禁忌、注意事项，
 * 并提供缺失字段检测、服用次数解析、结果格式化等纯函数能力。</p>
 */
public final class OcrDrugParser {

    private static final String TAG = "OcrDrugParser";

    private OcrDrugParser() {
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

    /** 检查药品信息缺失字段，返回缺失项列表 */
    public static List<String> findMissingFields(Drug drug) {
        List<String> missingList = new ArrayList<>();
        if (drug == null) {
            missingList.add("药品名称");
            missingList.add("功能主治");
            missingList.add("用法用量");
            missingList.add("不良反应");
            missingList.add("禁忌");
            missingList.add("注意事项");
            return missingList;
        }

        if (TextUtils.isEmpty(drug.getDrugName()) || "未知药品_".contains(drug.getDrugName()) || "未识别到药品名称".equals(drug.getDrugName())) {
            missingList.add("药品名称");
        }
        if ("未识别".equals(drug.getFunction())) {
            missingList.add("功能主治");
        }
        if ("未识别".equals(drug.getUsage())) {
            missingList.add("用法用量");
        }
        if ("未识别".equals(drug.getAdverseReaction())) {
            missingList.add("不良反应");
        }
        if ("未识别".equals(drug.getTaboo())) {
            missingList.add("禁忌");
        }
        if ("未识别".equals(drug.getNotice())) {
            missingList.add("注意事项");
        }
        return missingList;
    }

    /** 将药品结构化信息格式化为可读文本 */
    public static String formatResult(Drug drug) {
        StringBuilder sb = new StringBuilder();
        sb.append("【药品名称】").append(drug.getDrugName()).append("\n\n");
        sb.append("【功能主治】").append(drug.getFunction()).append("\n\n");
        sb.append("【用法用量】").append(drug.getUsage()).append("\n\n");
        sb.append("【不良反应】").append(drug.getAdverseReaction()).append("\n\n");
        sb.append("【禁忌】").append(drug.getTaboo()).append("\n\n");
        sb.append("【注意事项】").append(drug.getNotice());
        return sb.toString();
    }

    /** 解析药品说明书 OCR 文本，返回 Drug 对象 */
    public static Drug parse(String ocrText) {
        try {
            Drug drug = new Drug();
            if (TextUtils.isEmpty(ocrText)) {
                drug.setDrugName("未识别到药品名称");
                drug.setTakeTimes(1);
                drug.setFunction("未识别");
                drug.setUsage("未识别");
                drug.setAdverseReaction("未识别");
                drug.setTaboo("未识别");
                drug.setNotice("未识别");
                return drug;
            }

            String[] lines = ocrText.split("\n");
            List<String> validLines = new ArrayList<>();
            for (String line : lines) {
                String trimLine = line.trim();
                if (!trimLine.isEmpty() && !trimLine.equals("【】") && !trimLine.equals("()")) {
                    validLines.add(trimLine);
                }
            }

            final String KEY_NAME = "通用名称|药品名称|商品名|品名";
            final String KEY_FUNCTION = "功能主治|功能与主治|适应症";
            final String KEY_USAGE = "用法用量|用法|用量|服用方法";
            final String KEY_REACTION = "不良反应|不良事件";
            final String KEY_TABOO = "禁忌|禁用";
            final String KEY_NOTICE = "注意事项|用药须知";

            String drugName = "";
            StringBuilder function = new StringBuilder();
            StringBuilder usage = new StringBuilder();
            StringBuilder reaction = new StringBuilder();
            StringBuilder taboo = new StringBuilder();
            StringBuilder notice = new StringBuilder();

            for (String line : validLines) {
                if (line.matches(".*(" + KEY_NAME + ").*")) {
                    drugName = line.replaceAll(KEY_NAME, "").replace("：", "").replace(":", "").trim();
                    drugName = drugName.replace("【", "").replace("】", "").replace("(", "").replace(")", "").replace("说明书", "").trim();
                    break;
                }
            }

            if (TextUtils.isEmpty(drugName) || drugName.contains("药业") || drugName.contains("公司")
                    || drugName.contains("集团") || drugName.contains("厂") || drugName.length() < 2) {
                for (String line : validLines) {
                    if (line.contains("功能主治") || line.contains("用法用量") || line.contains("不良反应")
                            || line.contains("禁忌") || line.contains("注意事项") || line.contains("贮藏")
                            || line.contains("生产企业") || line.contains("药业") || line.contains("公司")
                            || line.contains("集团") || line.contains("厂") || line.contains("批准文号")
                            || line.contains("核准日期") || line.contains("日期") || line.contains("规格")
                            || line.contains("国药准字") || line.contains("批号") || line.contains("有效期")) {
                        continue;
                    }
                    if (line.length() >= 2 && line.length() <= 25
                            && !line.matches(".*\\d{4}年\\d{2}月\\d{2}日.*")
                            && !line.matches(".*\\d{4}-\\d{2}-\\d{2}.*")
                            && !line.matches(".*[0-9]{6,}.*")
                            && !line.matches(".*[\\\\/:*?\"<>|].*")) {
                        drugName = line.replace("【", "").replace("】", "").replace("(", "").replace(")", "").replace("说明书", "").trim();
                        break;
                    }
                }
                if (TextUtils.isEmpty(drugName) || drugName.contains("药业") || drugName.contains("公司") || drugName.length() < 2) {
                    drugName = "未识别到药品名称";
                }
            }

            String currentSection = "";
            for (String line : validLines) {
                if (line.matches(".*(" + KEY_FUNCTION + ").*")) {
                    currentSection = "function";
                    String content = line.replaceAll(KEY_FUNCTION, "").replace("：", "").replace(":", "").trim();
                    if (!content.isEmpty()) function.append(content).append("\n");
                } else if (line.matches(".*(" + KEY_USAGE + ").*")) {
                    currentSection = "usage";
                    String content = line.replaceAll(KEY_USAGE, "").replace("：", "").replace(":", "").trim();
                    if (!content.isEmpty()) usage.append(content).append("\n");
                } else if (line.matches(".*(" + KEY_REACTION + ").*")) {
                    currentSection = "reaction";
                    String content = line.replaceAll(KEY_REACTION, "").replace("：", "").replace(":", "").trim();
                    if (!content.isEmpty()) reaction.append(content).append("\n");
                } else if (line.matches(".*(" + KEY_TABOO + ").*")) {
                    currentSection = "taboo";
                    String content = line.replaceAll(KEY_TABOO, "").replace("：", "").replace(":", "").trim();
                    if (!content.isEmpty()) taboo.append(content).append("\n");
                } else if (line.matches(".*(" + KEY_NOTICE + ").*")) {
                    currentSection = "notice";
                    String content = line.replaceAll(KEY_NOTICE, "").replace("：", "").replace(":", "").trim();
                    if (!content.isEmpty()) notice.append(content).append("\n");
                } else if (line.contains("【规格】") || line.contains("规格")
                        || line.contains("【不良反应】") || line.contains("不良反应")
                        || line.contains("【禁忌】") || line.contains("禁忌")
                        || line.contains("【注意事项】") || line.contains("注意事项")
                        || line.contains("【药理毒理】") || line.contains("药理毒理")
                        || line.contains("【贮藏】") || line.contains("贮藏")
                        || line.contains("【包装】") || line.contains("包装")
                        || line.contains("【有效期】") || line.contains("有效期")
                        || line.contains("【执行标准】") || line.contains("执行标准")
                        || line.contains("【批准文号】") || line.contains("批准文号")
                        || line.contains("【生产企业】") || line.contains("生产企业")) {
                    currentSection = "";
                } else if (!currentSection.isEmpty()) {
                    String content = line.replace("【", "").replace("】", "").replace("(", "").replace(")", "").trim();
                    if (!content.isEmpty()) {
                        switch (currentSection) {
                            case "function": function.append(content).append("\n"); break;
                            case "usage": usage.append(content).append("\n"); break;
                            case "reaction": reaction.append(content).append("\n"); break;
                            case "taboo": taboo.append(content).append("\n"); break;
                            case "notice": notice.append(content).append("\n"); break;
                        }
                    }
                }
            }

            int takeTimes = extractTakeTimes(usage.toString());

            drug.setDrugName(drugName);
            drug.setFunction(function.length() > 0 ? function.toString().trim() : "未识别");
            drug.setUsage(usage.length() > 0 ? usage.toString().trim() : "未识别");
            drug.setAdverseReaction(reaction.length() > 0 ? reaction.toString().trim() : "未识别");
            drug.setTaboo(taboo.length() > 0 ? taboo.toString().trim() : "未识别");
            drug.setNotice(notice.length() > 0 ? notice.toString().trim() : "未识别");
            drug.setTakeTimes(takeTimes);
            drug.setTakeStatus("未服用");
            drug.setCount(0);
            drug.setRemindEnabled(false);
            drug.setRemindTime(0);

            return drug;
        } catch (Exception e) {
            Log.e(TAG, "解析Drug失败：" + e.getMessage());
            Drug drug = new Drug();
            drug.setDrugName("未识别到药品名称");
            drug.setTakeTimes(1);
            drug.setFunction("未识别");
            drug.setUsage("未识别");
            drug.setAdverseReaction("未识别");
            drug.setTaboo("未识别");
            drug.setNotice("未识别");
            return drug;
        }
    }

    /** 从用法用量文本中提取每日服用次数 */
    public static int extractTakeTimes(String usage) {
        if (TextUtils.isEmpty(usage) || "未识别".equals(usage)) return 1;
        String[] patterns = {
                "一日(\\d+)次", "每日(\\d+)次", "1日(\\d+)次", "每天(\\d+)次",
                "一日(\\d+)回", "每日(\\d+)回", "1日(\\d+)回", "每天(\\d+)回"
        };
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(usage);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception e) {
                    return 1;
                }
            }
        }
        return 1;
    }
}
