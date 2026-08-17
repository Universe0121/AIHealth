package com.aihealth.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class DrugStatusCount {
    @ColumnInfo(name = "takeStatus")
    private String takeStatus;

    @ColumnInfo(name = "count")
    private int count;

    public DrugStatusCount() {
        // Room需要的无参构造函数
    }

    @Ignore
    public DrugStatusCount(String takeStatus, int count) {
        this.takeStatus = takeStatus;
        this.count = count;
    }

    public String getTakeStatus() {
        return takeStatus;
    }

    public void setTakeStatus(String takeStatus) {
        this.takeStatus = takeStatus;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}