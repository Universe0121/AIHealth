package com.aihealth.ui.activity;

import com.aihealth.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.aihealth.data.db.AppDatabase;

/**
 * AI健康助手启动页：初始化数据库 + 延时跳主页面
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // 启动页布局

        // 子线程初始化Room数据库（避免主线程阻塞）
        new Thread(() -> AppDatabase.getInstance(this)).start();

        // 延时2秒跳转到MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 关闭启动页
        }, 2000);
    }
}