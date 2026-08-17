package com.aihealth.data.model;

// 轻量数据类，仅用于查询多时间字段
public class DrugTimeInfo {
    private int id;
    private String drugName;
    private String takeTimeList;

    public DrugTimeInfo(int id, String drugName, String takeTimeList) {
        this.id = id;
        this.drugName = drugName;
        this.takeTimeList = takeTimeList;
    }

    public int getId() {
        return id;
    }

    public String getDrugName() {
        return drugName;
    }

    public String getTakeTimeList() {
        return takeTimeList;
    }
}