package com.aihealth.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.aihealth.data.dao.AppDao;
import com.aihealth.data.dao.DiagnosisDao;
import com.aihealth.data.dao.DietDao;
import com.aihealth.data.dao.UserDao;
import com.aihealth.data.entity.DiagnosisEntity;
import com.aihealth.data.entity.DietRecord;
import com.aihealth.data.entity.Drug;
import com.aihealth.data.entity.NutritionItem;
import com.aihealth.data.entity.SportRecord;
import com.aihealth.data.entity.User;

/**
 * 应用唯一的 Room 数据库。
 *
 * <p>由原 AppDatabaseA（药品/运动/诊断）、AppDatabaseC（饮食/营养）、AppDatabaseUser（用户）
 * 三个数据库合并而来，统一管理全部实体与迁移。</p>
 */
@Database(
        entities = {
                Drug.class,
                SportRecord.class,
                DiagnosisEntity.class,
                DietRecord.class,
                NutritionItem.class,
                User.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "aihealth_db";

    private static volatile AppDatabase INSTANCE;

    /** 药品 / 运动 / 诊断单相关查询 */
    public abstract AppDao appDao();

    /** 诊断单相关查询 */
    public abstract DiagnosisDao diagnosisDao();

    /** 饮食记录 / 营养数据相关查询 */
    public abstract DietDao dietDao();

    /** 用户相关查询 */
    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    seedNutritionData();
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 首次创建数据库时，预置常用食物的营养成分数据。
     * 在后台线程执行，避免阻塞主线程。
     */
    private static void seedNutritionData() {
        new Thread(() -> {
            DietDao dietDao = INSTANCE.dietDao();
            if (dietDao.getAllNutritionItems() != null && !dietDao.getAllNutritionItems().isEmpty()) {
                return;
            }
            insertNutrition(dietDao, "米饭", 130.0, 2.6, 0.3, 28.2);
            insertNutrition(dietDao, "鸡胸肉", 165.0, 31.0, 3.6, 0.0);
            insertNutrition(dietDao, "鸡蛋", 155.0, 13.0, 11.0, 1.1);
            insertNutrition(dietDao, "西兰花", 34.0, 2.8, 0.4, 6.6);
            insertNutrition(dietDao, "胡萝卜", 41.0, 0.9, 0.2, 9.6);
            insertNutrition(dietDao, "面包", 265.0, 9.0, 3.2, 49.0);
            insertNutrition(dietDao, "苹果", 52.0, 0.3, 0.2, 13.8);
            insertNutrition(dietDao, "牛肉", 250.0, 26.0, 15.0, 0.0);
            insertNutrition(dietDao, "牛奶", 54.0, 3.5, 1.0, 5.0);
            insertNutrition(dietDao, "豆腐", 76.0, 8.1, 4.2, 1.9);
            insertNutrition(dietDao, "鱼肉", 120.0, 20.0, 4.0, 0.0);
            insertNutrition(dietDao, "土豆", 77.0, 2.0, 0.1, 17.0);
            insertNutrition(dietDao, "西红柿", 18.0, 0.9, 0.2, 3.9);
            insertNutrition(dietDao, "黄瓜", 15.0, 0.7, 0.1, 3.6);
            insertNutrition(dietDao, "香蕉", 93.0, 1.1, 0.2, 23.0);
            insertNutrition(dietDao, "橙子", 47.0, 0.9, 0.1, 11.8);
        }).start();
    }

    private static void insertNutrition(DietDao dao, String name, double calories,
                                        double protein, double fat, double carbs) {
        NutritionItem item = new NutritionItem();
        item.setFoodName(name);
        item.setCaloriesPer100g(calories);
        item.setProtein(protein);
        item.setFat(fat);
        item.setCarbohydrates(carbs);
        dao.insertNutritionItem(item);
    }
}
