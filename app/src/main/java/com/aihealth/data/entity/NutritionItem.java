package com.aihealth.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "nutrition_data")
public class NutritionItem {
    @PrimaryKey
    @NonNull
    public String foodName = "";

    public double caloriesPer100g = 0.0;
    public double protein = 0.0;
    public double fat = 0.0;
    public double carbohydrates = 0.0;

    // Room需要的无参构造方法
    public NutritionItem() {
    }

    // Getter和Setter
    @NonNull
    public String getFoodName() { return foodName; }
    public void setFoodName(@NonNull String foodName) { this.foodName = foodName; }

    public double getCaloriesPer100g() { return caloriesPer100g; }
    public void setCaloriesPer100g(double caloriesPer100g) { this.caloriesPer100g = caloriesPer100g; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public double getCarbohydrates() { return carbohydrates; }
    public void setCarbohydrates(double carbohydrates) { this.carbohydrates = carbohydrates; }
}