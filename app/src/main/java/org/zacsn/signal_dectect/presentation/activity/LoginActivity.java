package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.api.LoginResponse;
import org.zacsn.signal_dectect.presentation.viewmodel.LoginViewModel;
import org.zacsn.signal_dectect.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        ImageView ivBack = findViewById(R.id.iv_back);
        TextInputEditText etUsername = findViewById(R.id.et_username);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        SessionManager sessionManager = new SessionManager(this);

        ivBack.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String machineCode = sessionManager.getMachineCode();

            viewModel.login(username, password, machineCode);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                btnLogin.setEnabled(false);
                btnLogin.setText("");
                progressBar.setVisibility(View.VISIBLE);
            } else {
                btnLogin.setEnabled(true);
                btnLogin.setText("登 录");
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getLoginResult().observe(this, result -> {
            if (result != null && !result.isEmpty()) {
                if ("登录成功".equals(result)) {
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                } else {
                    showLoginErrorDialog(result);
                }
            }
        });

        viewModel.getIsLoginSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                // Save session
                String username = etUsername.getText() != null ? etUsername.getText().toString() : "Admin";
                LoginResponse.Data loginData = viewModel.getLoginData();
                if (loginData != null) {
                    sessionManager.createLoginSession(
                            username,
                            loginData.getUserId(),
                            loginData.getNickname(),
                            loginData.getToken(),
                            loginData.getValidUntil(),
                            loginData.getMachineCode(),
                            loginData.getMaxMachineBindings()
                    );
                } else {
                    sessionManager.createLoginSession(username);
                }
                
                // Start MainActivity
                android.content.Intent intent = new android.content.Intent(this, org.zacsn.signal_dectect.MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showLoginErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("登录失败")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }
}
