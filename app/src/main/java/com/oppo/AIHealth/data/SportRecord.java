package com.oppo.AIHealth.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sport_record")
public class SportRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String sportTime; // 改为sportTime（匹配代码中的getSportTime）
    private int duration;     // 运动时长（分钟）
    private int avgHeartRate; // 平均心率

    // 无参构造（Room必需）
    public SportRecord() {}

    // 带参构造（忽略）
    @Ignore
    public SportRecord(String sportTime, int duration, int avgHeartRate) {
        this.sportTime = sportTime;
        this.duration = duration;
        this.avgHeartRate = avgHeartRate;
    }

    // ========== 补充代码中调用的所有get/set方法 ==========
    public String getSportTime() {
        return sportTime;
    }

    public void setSportTime(String sportTime) {
        this.sportTime = sportTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getAvgHeartRate() {
        return avgHeartRate;
    }

    public void setAvgHeartRate(int avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    // 其他基础get/set（可选）
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}