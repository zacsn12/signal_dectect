package org.zacsn.signal_dectect.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.ChangePasswordRequest;
import org.zacsn.signal_dectect.data.api.LoginResponse;
import org.zacsn.signal_dectect.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class ChangePasswordActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private AuthApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);
        apiService = AuthApiConfig.createService();

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

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = sessionManager.getToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "登录已失效，请重新登录", Toast.LENGTH_LONG).show();
                goToLogin();
                return;
            }

            btnSubmit.setEnabled(false);
            apiService.changePassword(
                    "Bearer " + token,
                    new ChangePasswordRequest(oldPassword, newPassword)
            ).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    btnSubmit.setEnabled(true);
                    if (!response.isSuccessful() || response.body() == null) {
                        showErrorDialog(
                                "密码修改失败",
                                "接口地址: " + AuthApiConfig.BASE_URL + "api/auth/change-password\n"
                                        + "HTTP 状态码: " + response.code() + "\n"
                                        + "请确认后端已部署最新 server.js，并已重启服务。"
                        );
                        return;
                    }

                    LoginResponse result = response.body();
                    Toast.makeText(ChangePasswordActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                    if (result.getCode() == 200 || result.getCode() == 401) {
                        sessionManager.logout();
                        goToLogin();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    btnSubmit.setEnabled(true);
                    showErrorDialog(
                            "密码修改失败",
                            "接口地址: " + AuthApiConfig.BASE_URL + "api/auth/change-password\n\n"
                                    + "错误类型: " + t.getClass().getName() + "\n"
                                    + "错误信息: " + String.valueOf(t.getMessage()) + "\n\n"
                                    + "排查方向:\n"
                                    + "1. 服务器是否正在监听 1234 端口\n"
                                    + "2. 云服务器安全组/防火墙是否放行 TCP 1234\n"
                                    + "3. 手机当前网络是否能访问该公网 IP\n"
                                    + "4. 后端是否已重启到最新代码"
                    );
                }
            });
        });
    }

    private void showErrorDialog(String title, String message) {
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(message);
        textView.setTextIsSelectable(true);
        textView.setTextSize(14);
        scrollView.addView(textView);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
