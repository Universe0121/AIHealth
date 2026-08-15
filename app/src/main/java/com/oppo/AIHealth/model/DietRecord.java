package com.oppo.AIHealth.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import java.util.Date;

@Entity(tableName = "diet_records")
public class DietRecord {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;

    public String foodItems = "";  // 识别出的食材
    public double calories = 0.0;  // 总热量
    public String imagePath = "";  // 图片路径
    public Date timestamp = new Date();  // 记录时间

    // Room需要的无参构造方法
    public DietRecord() {
    }

    // Getter和Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFoodItems() { return foodItems; }
    public void setFoodItems(String foodItems) { this.foodItems = foodItems; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}