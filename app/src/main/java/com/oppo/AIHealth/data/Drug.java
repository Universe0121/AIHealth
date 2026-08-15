package com.oppo.AIHealth.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

// Room实体类：药品信息
@Entity(tableName = "drug")
public class Drug {
    @PrimaryKey(autoGenerate = true)
    private int id; // 主键
    private String drugName; // 药品名称
    private String takeTime; // 服用时间（如"08:00"）- 兼容原有单时间逻辑
    private int takeTimes; // 今日服用次数
    private String takeStatus; // 服用状态（未服用/已服用）
    private String createTime; // 创建时间
    // 🔥 新增：count字段（解决setCount调用报错）
    private int count; // 药品数量/计数
    // 新增：OCR识别字段
    private String function;      // 功能主治
    private String usage;         // 用法用量（OCR识别后可手动修改）
    private String adverseReaction; // 不良反应
    private String taboo;         // 禁忌
    private String notice;        // 注意事项

    // 新增：诊断单联动字段
    private int diagnosisId = -1; // 关联的诊断单ID，-1表示未关联
    private boolean isSyncWithDiagnosis = false; // 是否与诊断单用量同步

    // 新增：提醒字段（与原有takeTime等字段兼容）
    private long remindTime = 0;  // 提醒时间戳
    private long calendarEventId = -1; // 日历事件ID
    private int alarmId = -1;     // 闹钟ID
    private boolean isRemindEnabled = false; // 是否开启提醒

    // ================ 核心新增：多服用时间存储字段 ================
    private String takeTimeList; // 多服用时间，逗号分隔（如：08:00,12:00,18:00）

    // Room推荐的无参构造器（必须保留）
    public Drug() {
    }

    // 带参构造器，用@Ignore避免Room混淆
    @Ignore
    public Drug(String drugName, String takeTime, int takeTimes, String takeStatus) {
        this.drugName = drugName;
        this.takeTime = takeTime;
        this.takeTimes = takeTimes;
        this.takeStatus = takeStatus;
    }

    // 新增：OCR识别后使用的带参构造
    @Ignore
    public Drug(String drugName, String function, String usage,
                String adverseReaction, String taboo, String notice) {
        this.drugName = drugName;
        this.function = function;
        this.usage = usage;
        this.adverseReaction = adverseReaction;
        this.taboo = taboo;
        this.notice = notice;
    }

    // 所有字段的get/set方法（补充缺失的方法）
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getTakeTime() {
        return takeTime;
    }

    public void setTakeTime(String takeTime) {
        this.takeTime = takeTime;
    }

    public int getTakeTimes() { // 补充缺失的get方法
        return takeTimes;
    }

    public void setTakeTimes(int takeTimes) { // 补充缺失的set方法
        this.takeTimes = takeTimes;
    }

    public String getTakeStatus() { // 补充缺失的get方法
        return takeStatus;
    }

    public void setTakeStatus(String takeStatus) { // 补充缺失的set方法
        this.takeStatus = takeStatus;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    // 🔥 新增：count字段的get/set（解决setCount调用报错）
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // 新增：OCR字段的get/set
    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getAdverseReaction() {
        return adverseReaction;
    }

    public void setAdverseReaction(String adverseReaction) {
        this.adverseReaction = adverseReaction;
    }

    public String getTaboo() {
        return taboo;
    }

    public void setTaboo(String taboo) {
        this.taboo = taboo;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    // 新增：联动字段的get/set
    public int getDiagnosisId() {
        return diagnosisId;
    }

    public void setDiagnosisId(int diagnosisId) {
        this.diagnosisId = diagnosisId;
    }

    public boolean isSyncWithDiagnosis() {
        return isSyncWithDiagnosis;
    }

    public void setSyncWithDiagnosis(boolean syncWithDiagnosis) {
        isSyncWithDiagnosis = syncWithDiagnosis;
    }

    // 新增：提醒字段的get/set
    public long getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(long remindTime) {
        this.remindTime = remindTime;
    }

    public long getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(long calendarEventId) {
        this.calendarEventId = calendarEventId;
    }

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public boolean isRemindEnabled() {
        return isRemindEnabled;
    }

    public void setRemindEnabled(boolean remindEnabled) {
        isRemindEnabled = remindEnabled;
    }

    // ================ 核心新增：多服用时间的get/set方法 ================
    // 1. 基础get/set（供Room数据库读写）
    public String getTakeTimeList() {
        return takeTimeList;
    }

    public void setTakeTimeList(String takeTimeList) {
        this.takeTimeList = takeTimeList;
    }

    // 2. 便捷方法：将List<String>转为逗号分隔的字符串（存入数据库）
    @Ignore // 标记为非Room持久化方法
    public void setTakeTimeListFromList(List<String> timeList) {
        if (timeList == null || timeList.isEmpty()) {
            this.takeTimeList = "";
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timeList.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(timeList.get(i));
        }
        this.takeTimeList = sb.toString();
    }

    // 3. 便捷方法：将逗号分隔的字符串转为List<String>（从数据库读取）
    @Ignore // 标记为非Room持久化方法
    public List<String> getTakeTimeListAsList() {
        List<String> timeList = new ArrayList<>();
        if (takeTimeList == null || takeTimeList.isEmpty()) {
            // 无多时间时，返回原有单时间
            if (takeTime != null && !takeTime.isEmpty()) {
                timeList.add(takeTime);
            }
            return timeList;
        }
        // 拆分字符串为时间列表
        String[] times = takeTimeList.split(",");
        for (String time : times) {
            if (time != null && !time.isEmpty()) {
                timeList.add(time.trim());
            }
        }
        return timeList;
    }
}