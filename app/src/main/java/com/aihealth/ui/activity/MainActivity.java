package com.aihealth.ui.activity;

import com.aihealth.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.aihealth.ui.fragment.DiagnosisFragment;
import com.aihealth.ui.fragment.DietAnalysisFragment;
import com.aihealth.ui.fragment.DrugManagementFragment;
import com.aihealth.ui.fragment.SportGuideFragment;
import com.aihealth.ui.fragment.VisualizationFragment;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNav = findViewById(R.id.bottom_nav);
        navView = findViewById(R.id.nav_view);

        // 设置监听器
        bottomNav.setOnNavigationItemSelectedListener(this);
        navView.setNavigationItemSelectedListener(this);

        // 设置Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 获取用户名并更新侧边栏
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "健康用户");
        // 获取侧边栏头部视图
        View headerView = navView.getHeaderView(0); // 获取第一个头部布局
        TextView tvUsername = headerView.findViewById(R.id.tv_username);
        TextView tvEmail = headerView.findViewById(R.id.tv_email);
        if (tvUsername != null) {
            tvUsername.setText(username);
        }
        if (tvEmail != null) {
            tvEmail.setText(username + "@aihealth.com");
        }

        // 默认显示第一个Fragment
        if (savedInstanceState == null) {
            loadFragment(new DiagnosisFragment());
            bottomNav.setSelectedItemId(R.id.nav_diagnosis);
        }

        // 处理返回按钮
        setupBackPressedHandler();
    }

    private void setupBackPressedHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // 加载Fragment
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    // 底部导航点击事件
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_diagnosis) {
            loadFragment(new DiagnosisFragment());
            setTitle("诊断单识别");
            return true;
        } else if (id == R.id.nav_drug) {
            loadFragment(new DrugManagementFragment());
            setTitle("药品管理");
            return true;
        } else if (id == R.id.nav_diet) {
            loadFragment(new DietAnalysisFragment());
            setTitle("饮食分析");
            return true;
        } else if (id == R.id.nav_sport) {
            loadFragment(new SportGuideFragment());
            setTitle("运动指导");
            return true;
        } else if (id == R.id.nav_visualization) {
            loadFragment(new VisualizationFragment());
            setTitle("数据可视化");
            return true;
        }

        // 处理侧边栏点击
        handleDrawerItem(item);
        return true;
    }

    // 侧边导航点击处理
    private void handleDrawerItem(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            Toast.makeText(this, "个人资料", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "帮助与反馈", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "关于AI健康管家", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            showLogoutConfirmDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    // 显示退出登录确认对话框
    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    sharedPreferences.edit().clear().apply();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}