package com.oppo.AIHealth.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * OPPO SDK工具类（模拟实现）
 */
public class OppoSdkUtil {
    private static Context mContext;
    private static Thread heartRateThread;
    private static Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler

    public static void init(Context context) {
        mContext = context.getApplicationContext();
        Toast.makeText(mContext, "工具类初始化成功", Toast.LENGTH_SHORT).show();
    }

    public static void recognizeDrugBox(String photoPath, final OcrResultListener listener) {
        Toast.makeText(mContext, "正在识别药盒...", Toast.LENGTH_SHORT).show();
        // 模拟识别结果
        mainHandler.postDelayed(() -> {
            String drugName = "布洛芬缓释胶囊";
            listener.onSuccess(drugName);
            Toast.makeText(mContext, "识别成功：" + drugName, Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    public static void connectBand(final HeartRateListener listener) {
        if (mContext == null) {
            listener.onFailure("工具类未初始化");
            return;
        }
        Toast.makeText(mContext, "正在连接手环...", Toast.LENGTH_SHORT).show();

        // 模拟连接成功
        mainHandler.postDelayed(() -> {
            Toast.makeText(mContext, "手环连接成功！", Toast.LENGTH_SHORT).show();

            // 停止旧线程
            if (heartRateThread != null && heartRateThread.isAlive()) {
                heartRateThread.interrupt();
            }

            // 模拟心率获取
            heartRateThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // 模拟随机心率
                        int heartRate = 60 + (int) (Math.random() * 40);
                        // 主线程回调（修复post方法错误）
                        mainHandler.post(() -> listener.onSuccess(heartRate));
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        mainHandler.post(() -> listener.onFailure("心率监听已停止"));
                        break;
                    }
                }
            });
            heartRateThread.start();
        }, 1000);
    }

    public static void stopHeartRateListen() {
        if (heartRateThread != null && heartRateThread.isAlive()) {
            heartRateThread.interrupt();
        }
    }

    public interface OcrResultListener {
        void onSuccess(String drugName);
        void onFailure(String error);
    }

    public interface HeartRateListener {
        void onSuccess(int heartRate);
        void onFailure(String error);
    }
}