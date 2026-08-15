package com.oppo.AIHealth.data;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.List;
import androidx.room.TypeConverters;
import android.content.Context;
import com.oppo.AIHealth.model.DietRecord;
import com.oppo.AIHealth.model.NutritionItem;
import com.oppo.AIHealth.utils.Converters;  // 添加这行导入

@Database(entities = {DietRecord.class, NutritionItem.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})  // 添加这行注解
public abstract class AppDatabaseC extends RoomDatabase {
    public abstract DietDao dietDao();

    private static volatile AppDatabaseC INSTANCE;

    public static AppDatabaseC getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabaseC.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabaseC.class, "health_database")
                            .fallbackToDestructiveMigration()
                            .build();

                    // 初始化营养数据
                    initializeNutritionData(INSTANCE);
                }
            }
        }
        return INSTANCE;
    }

    private static void initializeNutritionData(final AppDatabaseC database) {
        new Thread(() -> {
            // 先检查是否已经有数据，避免重复初始化
            List<NutritionItem> existingItems = database.dietDao().getAllNutritionItems();
            if (existingItems != null && !existingItems.isEmpty()) {
                return; // 如果已经有数据，不再初始化
            }

            NutritionItem rice = new NutritionItem();
            rice.setFoodName("米饭");  // 明确设置值
            rice.setCaloriesPer100g(130.0);
            rice.setProtein(2.6);
            rice.setFat(0.3);
            rice.setCarbohydrates(28.2);
            database.dietDao().insertNutritionItem(rice);

            NutritionItem chicken = new NutritionItem();
            chicken.setFoodName("鸡胸肉");
            chicken.setCaloriesPer100g(165.0);
            chicken.setProtein(31.0);
            chicken.setFat(3.6);
            chicken.setCarbohydrates(0.0);
            database.dietDao().insertNutritionItem(chicken);

            NutritionItem egg = new NutritionItem();
            egg.setFoodName("鸡蛋");
            egg.setCaloriesPer100g(155.0);
            egg.setProtein(13.0);
            egg.setFat(11.0);
            egg.setCarbohydrates(1.1);
            database.dietDao().insertNutritionItem(egg);

            NutritionItem broccoli = new NutritionItem();
            broccoli.setFoodName("西兰花");
            broccoli.setCaloriesPer100g(34.0);
            broccoli.setProtein(2.8);
            broccoli.setFat(0.4);
            broccoli.setCarbohydrates(6.6);
            database.dietDao().insertNutritionItem(broccoli);

            NutritionItem carrot = new NutritionItem();
            carrot.setFoodName("胡萝卜");
            carrot.setCaloriesPer100g(41.0);
            carrot.setProtein(0.9);
            carrot.setFat(0.2);
            carrot.setCarbohydrates(9.6);
            database.dietDao().insertNutritionItem(carrot);

            NutritionItem bread = new NutritionItem();
            bread.setFoodName("面包");
            bread.setCaloriesPer100g(265.0);
            bread.setProtein(9.0);
            bread.setFat(3.2);
            bread.setCarbohydrates(49.0);
            database.dietDao().insertNutritionItem(bread);

            NutritionItem apple = new NutritionItem();
            apple.setFoodName("苹果");
            apple.setCaloriesPer100g(52.0);
            apple.setProtein(0.3);
            apple.setFat(0.2);
            apple.setCarbohydrates(13.8);
            database.dietDao().insertNutritionItem(apple);

            // 添加更多食物
            NutritionItem beef = new NutritionItem();
            beef.setFoodName("牛肉");
            beef.setCaloriesPer100g(250.0);
            beef.setProtein(26.0);
            beef.setFat(15.0);
            beef.setCarbohydrates(0.0);
            database.dietDao().insertNutritionItem(beef);

            NutritionItem milk = new NutritionItem();
            milk.setFoodName("牛奶");
            milk.setCaloriesPer100g(54.0);
            milk.setProtein(3.5);
            milk.setFat(1.0);
            milk.setCarbohydrates(5.0);
            database.dietDao().insertNutritionItem(milk);

            NutritionItem tofu = new NutritionItem();
            tofu.setFoodName("豆腐");
            tofu.setCaloriesPer100g(76.0);
            tofu.setProtein(8.1);
            tofu.setFat(4.2);
            tofu.setCarbohydrates(1.9);
            database.dietDao().insertNutritionItem(tofu);

            NutritionItem fish = new NutritionItem();
            fish.setFoodName("鱼肉");
            fish.setCaloriesPer100g(120.0);
            fish.setProtein(20.0);
            fish.setFat(4.0);
            fish.setCarbohydrates(0.0);
            database.dietDao().insertNutritionItem(fish);

            NutritionItem potato = new NutritionItem();
            potato.setFoodName("土豆");
            potato.setCaloriesPer100g(77.0);
            potato.setProtein(2.0);
            potato.setFat(0.1);
            potato.setCarbohydrates(17.0);
            database.dietDao().insertNutritionItem(potato);

            NutritionItem tomato = new NutritionItem();
            tomato.setFoodName("西红柿");
            tomato.setCaloriesPer100g(18.0);
            tomato.setProtein(0.9);
            tomato.setFat(0.2);
            tomato.setCarbohydrates(3.9);
            database.dietDao().insertNutritionItem(tomato);

            NutritionItem cucumber = new NutritionItem();
            cucumber.setFoodName("黄瓜");
            cucumber.setCaloriesPer100g(15.0);
            cucumber.setProtein(0.7);
            cucumber.setFat(0.1);
            cucumber.setCarbohydrates(3.6);
            database.dietDao().insertNutritionItem(cucumber);

            NutritionItem banana = new NutritionItem();
            banana.setFoodName("香蕉");
            banana.setCaloriesPer100g(93.0);
            banana.setProtein(1.1);
            banana.setFat(0.2);
            banana.setCarbohydrates(23.0);
            database.dietDao().insertNutritionItem(banana);

            NutritionItem orange = new NutritionItem();
            orange.setFoodName("橙子");
            orange.setCaloriesPer100g(47.0);
            orange.setProtein(0.9);
            orange.setFat(0.1);
            orange.setCarbohydrates(11.8);
            database.dietDao().insertNutritionItem(orange);
        }).start();
    }

    // 辅助方法设置营养成分
    private static double getProtein(String foodName) {
        switch (foodName) {
            case "米饭": return 2.6;
            case "鸡胸肉": return 31.0;
            case "鸡蛋": return 13.0;
            case "牛奶": return 3.5;
            default: return 5.0;
        }
    }

    private static double getFat(String foodName) {
        switch (foodName) {
            case "米饭": return 0.3;
            case "鸡胸肉": return 3.6;
            case "鸡蛋": return 11.0;
            case "牛奶": return 1.0;
            default: return 2.0;
        }
    }

    private static double getCarbohydrates(String foodName) {
        switch (foodName) {
            case "米饭": return 28.2;
            case "鸡胸肉": return 0.0;
            case "鸡蛋": return 1.1;
            case "牛奶": return 5.0;
            default: return 10.0;
        }
    }
}