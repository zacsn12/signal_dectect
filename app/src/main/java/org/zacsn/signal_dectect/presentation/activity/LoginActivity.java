package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.presentation.viewmodel.LoginViewModel;
import org.zacsn.signal_dectect.util.LicenseManager;

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
        TextView tvMachineCode = findViewById(R.id.tv_machine_code);
        LicenseManager licenseManager = new LicenseManager(this);

        ivBack.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        btnLogin.setText("激活许可证");
        tvMachineCode.setText(licenseManager.getMachineCode());

        btnLogin.setOnClickListener(v -> {
            String licenseKey = etUsername.getText() != null ? etUsername.getText().toString() : "";
            String machineCode = licenseManager.getMachineCode();

            viewModel.activateLicense(licenseKey, machineCode);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                btnLogin.setEnabled(false);
                btnLogin.setText("");
                progressBar.setVisibility(View.VISIBLE);
            } else {
                btnLogin.setEnabled(true);
                btnLogin.setText("激活许可证");
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getLoginResult().observe(this, result -> {
            if (result != null && !result.isEmpty()) {
                if ("许可证激活成功".equals(result)) {
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                } else {
                    showLoginErrorDialog(result);
                }
            }
        });

        viewModel.getIsLoginSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                if (!licenseManager.saveLicense(viewModel.getLicenseData())) {
                    showLoginErrorDialog("许可证签名校验失败，请联系管理员重新签发许可证");
                    return;
                }
                android.content.Intent intent = new android.content.Intent(this, org.zacsn.signal_dectect.MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showLoginErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("许可证激活失败")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }
}
