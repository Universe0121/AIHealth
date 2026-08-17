package com.aihealth.ui.activity;

import com.aihealth.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.aihealth.data.db.AppDatabase;
import com.aihealth.data.entity.User;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private SharedPreferences sharedPreferences;
    private AppDatabase db;
    private TextInputLayout passwordLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
        if (isLoggedIn) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        passwordLayout = findViewById(R.id.password_layout);

        // 手动设置密码可见性切换
        setupPasswordToggle();

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void setupPasswordToggle() {
        passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
        passwordLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off); // 初始为隐藏图标
        passwordLayout.setEndIconOnClickListener(v -> {
            int selection = etPassword.getSelectionEnd();
            if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // 切换到显示密码
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility);
            } else {
                // 切换到隐藏密码
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordLayout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off);
            }
            etPassword.setSelection(selection);
        });
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().getUser(username, password);
            if (user != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("username", user.getUsername());
                editor.apply();
                runOnUiThread(() -> startMainActivity());
            } else {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}