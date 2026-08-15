//以下为原版加入ai和opencv代码，介于初赛没有ai服务可以用，我们进行了模拟数据来实现功能
//package com.oppo.aihealth.utils;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import com.oppo.aihealth.service.OppoAIService;
//import java.util.List;
//import java.util.Random;
//
//public class FoodRecognitionHelper {
//
//    /**
//     * 使用OPPO AI进行食材识别
//     * @param image 食物图片
//     * @param context 上下文
//     * @return 识别出的食材字符串（逗号分隔）
//     */
//    public static String recognizeFoodFromImage(Bitmap image, Context context) {
//        OppoAIService aiService = OppoAIService.getInstance(context);
//
//        // 检查AI服务是否可用
//        if (!aiService.isInitialized()) {
//            android.util.Log.w("FoodRecognitionHelper", "AI服务未初始化，使用模拟数据");
//            return getSimulatedFoods();
//        }
//
//        // 使用OPPO AI进行识别
//        List<OppoAIService.FoodRecognitionResult> results =
//                aiService.recognizeFoodSync(image);
//
//        // 处理识别结果
//        if (results == null || results.isEmpty()) {
//            android.util.Log.w("FoodRecognitionHelper", "未识别到食材，使用模拟数据");
//            return getSimulatedFoods();
//        }
//
//        // 过滤置信度大于0.5的结果
//        StringBuilder foodItems = new StringBuilder();
//        for (OppoAIService.FoodRecognitionResult result : results) {
//            if (result.confidence > 0.5f) { // 置信度阈值
//                if (foodItems.length() > 0) {
//                    foodItems.append(",");
//                }
//                foodItems.append(result.label);
//                android.util.Log.d("FoodRecognitionHelper",
//                        "识别到食材: " + result.label + ", 置信度: " + result.confidence);
//            }
//        }
//
//        // 如果没有高置信度结果，返回所有结果
//        if (foodItems.length() == 0 && !results.isEmpty()) {
//            foodItems.append(results.get(0).label);
//        }
//
//        return foodItems.toString();
//    }
//
//    /**
//     * 异步识别食材
//     */
//    public static void recognizeFoodFromImageAsync(Bitmap image, Context context,
//                                                   OppoAIService.RecognitionCallback callback) {
//        OppoAIService aiService = OppoAIService.getInstance(context);
//
//        if (!aiService.isInitialized()) {
//            if (callback != null) {
//                callback.onError("AI服务未初始化");
//            }
//            return;
//        }
//
//        aiService.recognizeFoodAsync(image, new OppoAIService.RecognitionCallback() {
//            @Override
//            public void onSuccess(List<OppoAIService.FoodRecognitionResult> results) {
//                if (callback != null) {
//                    // 转换为食材字符串
//                    StringBuilder foodItems = new StringBuilder();
//                    for (OppoAIService.FoodRecognitionResult result : results) {
//                        if (result.confidence > 0.5f) {
//                            if (foodItems.length() > 0) {
//                                foodItems.append(",");
//                            }
//                            foodItems.append(result.label);
//                        }
//                    }
//
//                    if (foodItems.length() > 0) {
//                        callback.onSuccess(results);
//                    } else {
//                        callback.onError("未识别到有效食材");
//                    }
//                }
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                if (callback != null) {
//                    callback.onError(errorMessage);
//                }
//            }
//        });
//    }
//
//    /**
//     * 模拟食材识别（备用方案）
//     */
//    private static String getSimulatedFoods() {
//        String[] commonFoods = {"米饭", "鸡胸肉", "鸡蛋", "西兰花", "胡萝卜",
//                "面包", "苹果", "牛肉", "牛奶", "豆腐"};
//        Random random = new Random();
//        int count = random.nextInt(3) + 1; // 1-3种食材
//
//        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < count; i++) {
//            if (i > 0) result.append(",");
//            result.append(commonFoods[random.nextInt(commonFoods.length)]);
//        }
//
//        return result.toString();
//    }
//
//    /**
//     * 计算食材热量
//     */
//    // 在 FoodRecognitionHelper.java 中，修改 calculateCalories 方法：
//    public static double calculateCalories(String foodItems, Context context) {
//        if (foodItems == null || foodItems.isEmpty()) return 0;
//
//        String[] foods = foodItems.split(",");
//        java.util.Random random = new java.util.Random();
//        double totalCalories = 0;
//
//        for (String food : foods) {
//            String trimmedFood = food.trim();
//            double weight = 100 + random.nextInt(200); // 100-300克
//            double caloriesPer100g = getBaseCalories(trimmedFood);
//            totalCalories += (caloriesPer100g * weight / 100);
//
//            android.util.Log.d("FoodRecognitionHelper",
//                    String.format("%s: %.2f千卡/100g × %.0fg = %.2f千卡",
//                            trimmedFood, caloriesPer100g, weight, caloriesPer100g * weight / 100));
//        }
//
//        return totalCalories;
//    }
//
//    private static double getBaseCalories(String foodName) {
//        // 基础热量数据库（每100克）
//        java.util.Map<String, Double> baseCalories = new java.util.HashMap<>();
//        baseCalories.put("米饭", 130.0);
//        baseCalories.put("鸡胸肉", 165.0);
//        baseCalories.put("鸡蛋", 155.0);
//        baseCalories.put("西兰花", 34.0);
//        baseCalories.put("胡萝卜", 41.0);
//        baseCalories.put("面包", 265.0);
//        baseCalories.put("苹果", 52.0);
//        baseCalories.put("牛肉", 250.0);
//        baseCalories.put("牛奶", 54.0);
//        baseCalories.put("豆腐", 76.0);
//
//        return baseCalories.getOrDefault(foodName.toLowerCase(), 100.0);
//    }
//}


package com.oppo.AIHealth.utils;

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