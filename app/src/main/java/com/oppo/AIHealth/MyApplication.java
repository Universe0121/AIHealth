package com.oppo.AIHealth;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * 全局Application（实例化调用OcrHelper）
 */
public class MyApplication extends Application {
    // 全局上下文（必须初始化）
    private static Context context;

    // 提供全局上下文获取方法
    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. 初始化全局上下文（第一步必须做）
        context = getApplicationContext();

        // 2. 初始化数据库（原有逻辑不变）
        com.oppo.AIHealth.data.AppDatabaseA.getInstance(this);

        // 3. ========== 核心：实例化OcrHelper并调用initOcrEngine ==========
        OcrHelper ocrHelper = new OcrHelper(); // 实例化
        boolean isOcrInitSuccess = ocrHelper.initOcrEngine(); // 调用实例方法
        if (isOcrInitSuccess) {
            Log.i("MyApplication", "OCR初始化成功");
        } else {
            Log.e("MyApplication", "OCR初始化失败");
        }

        // 4. 创建通知渠道（原有逻辑不变）
        createNotificationChannel();
    }

    /**
     * 创建通知渠道（原有方法不变）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "drug_channel",
                    "用药提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("接收药品服用提醒");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}