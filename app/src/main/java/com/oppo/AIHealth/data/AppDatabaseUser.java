package com.oppo.AIHealth.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class AppDatabaseUser extends RoomDatabase {
    public abstract UserDao userDao();

    private static volatile AppDatabaseUser INSTANCE;

    public static AppDatabaseUser getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabaseUser.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabaseUser.class, "user_database.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}