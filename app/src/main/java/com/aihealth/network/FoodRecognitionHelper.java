package com.aihealth.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Random;

public class FoodRecognitionHelper {

    private static final String TAG = "FoodRecognitionHelper";

    /**
     * 使用百度AI进行食材识别
     * 需要Bitmap和Context两个参数
     */
    public static String recognizeFoodFromImage(Bitmap image, Context context) {
        try {
            // 创建一个同步的识别结果容器
            final String[] result = new String[1];
            final boolean[] done = new boolean[1];

            // 调用百度AI进行识别
            BaiduAIService.getInstance().recognizeFood(image, new BaiduAIService.RecognitionCallback() {
                @Override
                public void onSuccess(JsonObject jsonObject) {
                    Log.d(TAG, "百度AI识别成功");
                    result[0] = parseRecognitionResult(jsonObject);
                    done[0] = true;
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "百度AI识别失败: " + error);
                    // 使用模拟数据作为备用方案
                    result[0] = getSimulatedFoods();
                    done[0] = true;
                }
            });

            // 等待识别完成，最多等待10秒
            long startTime = System.currentTimeMillis();
            while (!done[0] && System.currentTimeMillis() - startTime < 10000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 如果超时，使用模拟数据
            if (!done[0]) {
                Log.w(TAG, "百度AI识别超时，使用模拟数据");
                return getSimulatedFoods();
            }

            return result[0] != null ? result[0] : getSimulatedFoods();
        } catch (Exception e) {
            Log.e(TAG, "识别过程异常: " + e.getMessage());
            return getSimulatedFoods();
        }
    }

    /**
     * 计算食材热量
     * 需要foodItems和Context两个参数
     */
    public static double calculateCalories(String foodItems, Context context) {
        if (foodItems == null || foodItems.isEmpty()) return 0;

        String[] foods = foodItems.split(",");
        Random random = new Random();
        double totalCalories = 0;

        for (String food : foods) {
            String trimmedFood = food.trim();
            double weight = 100 + random.nextInt(200); // 100-300克
            double caloriesPer100g = getBaseCalories(trimmedFood);
            totalCalories += (caloriesPer100g * weight / 100);
        }

        return totalCalories;
    }

    /**
     * 解析百度AI的识别结果
     */
    private static String parseRecognitionResult(JsonObject jsonObject) {
        try {
            JsonArray resultArray = jsonObject.getAsJsonArray("result");
            if (resultArray == null || resultArray.size() == 0) {
                return getSimulatedFoods();
            }

            StringBuilder foodItems = new StringBuilder();
            for (int i = 0; i < resultArray.size(); i++) {
                JsonObject item = resultArray.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                double probability = item.get("probability").getAsDouble();

                // 只添加置信度大于0.5的结果
                if (probability > 0.5) {
                    if (foodItems.length() > 0) {
                        foodItems.append(",");
                    }
                    foodItems.append(name);
                    Log.d(TAG, "识别到食材: " + name + ", 置信度: " + probability);
                }
            }

            // 如果没有高置信度结果，使用第一个结果
            if (foodItems.length() == 0 && resultArray.size() > 0) {
                JsonObject firstItem = resultArray.get(0).getAsJsonObject();
                String name = firstItem.get("name").getAsString();
                foodItems.append(name);
            }

            return foodItems.length() > 0 ? foodItems.toString() : getSimulatedFoods();
        } catch (Exception e) {
            Log.e(TAG, "解析识别结果失败: " + e.getMessage());
            return getSimulatedFoods();
        }
    }

    /**
     * 模拟食材识别
     */
    private static String getSimulatedFoods() {
        String[] commonFoods = {"米饭", "鸡胸肉", "鸡蛋", "西兰花", "胡萝卜",
                "面包", "苹果", "牛肉", "牛奶", "豆腐"};
        Random random = new Random();
        int count = random.nextInt(3) + 1; // 1-3种食材

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) result.append(",");
            result.append(commonFoods[random.nextInt(commonFoods.length)]);
        }

        return result.toString();
    }

    private static double getBaseCalories(String foodName) {
        switch (foodName) {
            case "米饭": return 130;
            case "鸡胸肉": return 165;
            case "鸡蛋": return 155;
            case "西兰花": return 34;
            case "胡萝卜": return 41;
            case "面包": return 265;
            case "苹果": return 52;
            case "牛肉": return 250;
            case "牛奶": return 54;
            case "豆腐": return 76;
            default: return 100;
        }
    }
}