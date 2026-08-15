package com.oppo.AIHealth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.oppo.AIHealth.data.AppDatabaseUser;
import com.oppo.AIHealth.data.User;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private AppDatabaseUser dbUser;
    private TextInputLayout passwordLayout, confirmPasswordLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbUser = AppDatabaseUser.getInstance(this);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);

        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);

        // 设置密码可见性切换
        setupPasswordToggle(passwordLayout, etPassword);
        setupPasswordToggle(confirmPasswordLayout, etConfirmPassword);

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupPasswordToggle(TextInputLayout layout, TextInputEditText editText) {
        layout.setEndIconMode(TextInputLayout.END_ICON_NONE);
        layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off);
        layout.setEndIconOnClickListener(v -> {
            int selection = editText.getSelectionEnd();
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                layout.setEndIconDrawable(com.google.android.material.R.drawable.design_ic_visibility_off);
            }
            editText.setSelection(selection);
        });
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            User existingUser = dbUser.userDao().getUserByUsername(username);
            if (existingUser != null) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show());
                return;
            }

            User user = new User(username, password);
            dbUser.userDao().insert(user);

            runOnUiThread(() -> {
                Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        });
    }
}