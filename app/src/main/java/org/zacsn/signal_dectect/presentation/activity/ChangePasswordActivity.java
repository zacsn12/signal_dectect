package org.zacsn.signal_dectect.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.util.SessionManager;

public class ChangePasswordActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        TextInputEditText etOldPassword = findViewById(R.id.et_old_password);
        TextInputEditText etNewPassword = findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = findViewById(R.id.et_confirm_password);
        MaterialButton btnSubmit = findViewById(R.id.btn_submit);

        ivBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText() != null ? etOldPassword.getText().toString() : "";
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!oldPassword.equals(sessionManager.getPassword())) {
                Toast.makeText(this, "当前密码错误", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update password and logout
            sessionManager.updatePassword(newPassword);
            sessionManager.logout();

            Toast.makeText(this, "密码修改成功，请重新登录", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
