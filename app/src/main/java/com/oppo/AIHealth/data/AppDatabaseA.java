package com.oppo.AIHealth.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// 版本号从3升级为4（新增诊断单表）
@Database(entities = {Drug.class, SportRecord.class, DiagnosisEntity.class}, version = 4, exportSchema = true)
public abstract class AppDatabaseA extends RoomDatabase {

    // 单例实例
    private static volatile AppDatabaseA INSTANCE;

    // DAO获取方法
    public abstract AppDao appDao();
    public abstract DiagnosisDao diagnosisDao(); // 新增诊断单DAO

    // ========== 原有迁移规则：1→2（保留） ==========
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE drug ADD COLUMN function TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE drug ADD COLUMN usage TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE drug ADD COLUMN adverseReaction TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE drug ADD COLUMN taboo TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE drug ADD COLUMN notice TEXT DEFAULT ''");
            database.execSQL("ALTER TABLE drug ADD COLUMN diagnosisId INTEGER DEFAULT -1");
            database.execSQL("ALTER TABLE drug ADD COLUMN isSyncWithDiagnosis INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE drug ADD COLUMN remindTime INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE drug ADD COLUMN calendarEventId INTEGER DEFAULT -1");
            database.execSQL("ALTER TABLE drug ADD COLUMN alarmId INTEGER DEFAULT -1");
            database.execSQL("ALTER TABLE drug ADD COLUMN isRemindEnabled INTEGER DEFAULT 0");
        }
    };

    // ========== 原有迁移规则：2→3（添加多时间字段） ==========
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE drug ADD COLUMN takeTimeList TEXT DEFAULT ''");
        }
    };

    // ========== 新增迁移规则：3→4（创建诊断单表） ==========
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 创建诊断单表
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `diagnosis` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`ocrText` TEXT, " +
                            "`structuredData` TEXT, " + // 将存储JSON字符串
                            "`imagePath` TEXT, " +
                            "`imageSize` INTEGER NOT NULL DEFAULT 0, " +
                            "`imageDate` TEXT, " +
                            "`timestamp` INTEGER)" // 存储时间戳（毫秒）
            );
        }
    };

    // 单例创建方法（添加3→4迁移规则）
    public static AppDatabaseA getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabaseA.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabaseA.class,
                                    "aihealth_db"
                            )
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // 追加3→4迁移
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // 兼容代码中的getAppDao()调用
    public AppDao getAppDao() { return appDao(); }
}