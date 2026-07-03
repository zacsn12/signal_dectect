package org.zacsn.signal_dectect.presentation.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.LicenseApiService;
import org.zacsn.signal_dectect.data.api.LicenseRequest;
import org.zacsn.signal_dectect.data.api.LicenseResponse;
import org.zacsn.signal_dectect.util.LicenseManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthInfoActivity extends AppCompatActivity {
    private LicenseManager licenseManager;
    private TextView tvLocalMachineCode;
    private TextView tvAuthSummary;
    private TextView tvBindingCapacity;
    private TextView tvLocalBindingStatus;
    private TextView tvValidUntil;
    private TextView tvAuthStatus;
    
    // New UI Elements
    private TextView tvLicenseCustomer;
    private TextView tvLicenseKey;
    private TextView tvLicenseFeatures;
    private TextView tvSyncTip;
    private TextView btnCopyCode;
    private View vStatusDot;
    private FrameLayout flStatusIconBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_info);

        ImageView ivBack = findViewById(R.id.iv_back);
        tvLocalMachineCode = findViewById(R.id.tv_local_machine_code);
        tvAuthSummary = findViewById(R.id.tv_auth_summary);
        tvBindingCapacity = findViewById(R.id.tv_binding_capacity);
        tvLocalBindingStatus = findViewById(R.id.tv_local_binding_status);
        tvValidUntil = findViewById(R.id.tv_valid_until);
        tvAuthStatus = findViewById(R.id.tv_auth_status);
        
        tvLicenseCustomer = findViewById(R.id.tv_license_customer);
        tvLicenseKey = findViewById(R.id.tv_license_key);
        tvLicenseFeatures = findViewById(R.id.tv_license_features);
        tvSyncTip = findViewById(R.id.tv_sync_tip);
        btnCopyCode = findViewById(R.id.btn_copy_code);
        vStatusDot = findViewById(R.id.v_status_dot);
        flStatusIconBg = findViewById(R.id.fl_status_icon_bg);

        ivBack.setOnClickListener(v -> finish());
        
        licenseManager = new LicenseManager(this);
        renderLocalLicense();

        // Clicking the sync action tip refreshes the license
        tvSyncTip.setOnClickListener(v -> refreshLicense());
        
        // Copy machine code listener
        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("MachineCode", licenseManager.getMachineCode());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "设备机器码已复制", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderLocalLicense() {
        tvLocalMachineCode.setText(licenseManager.getMachineCode());
        tvValidUntil.setText(licenseManager.getValidUntil().isEmpty() ? "永不过期" : licenseManager.getValidUntil());
        tvBindingCapacity.setText("单机离线授权");
        
        tvLicenseCustomer.setText(licenseManager.getCustomerName().isEmpty() ? "未知授权客户" : licenseManager.getCustomerName());
        tvLicenseKey.setText(licenseManager.getLicenseKey().isEmpty() ? "暂无激活秘钥" : licenseManager.getLicenseKey());
        
        String features = licenseManager.getFeaturesText();
        tvLicenseFeatures.setText(features.isEmpty() ? "全部功能授权 (Bluetooth/WiFi/Cellular)" : features);

        String error = licenseManager.getValidationError();
        if (error.isEmpty()) {
            tvAuthStatus.setText("已授权");
            tvAuthStatus.setTextColor(getColor(R.color.success));
            tvLocalBindingStatus.setText("本机许可证有效，可离线使用");
            tvLocalBindingStatus.setTextColor(getColor(R.color.success));
            vStatusDot.setBackgroundResource(R.drawable.bg_pill_green);
            flStatusIconBg.setBackgroundResource(R.drawable.bg_icon_circle_green);
            tvAuthSummary.setText("当前设备已通过本地许可证数字签名校验。");
            tvSyncTip.setText("同步云端许可证");
        } else {
            tvAuthStatus.setText("未授权");
            tvAuthStatus.setTextColor(getColor(R.color.error));
            tvLocalBindingStatus.setText(error);
            tvLocalBindingStatus.setTextColor(getColor(R.color.error));
            vStatusDot.setBackgroundResource(R.drawable.bg_pill_rose);
            flStatusIconBg.setBackgroundResource(R.drawable.bg_icon_circle_red);
            tvAuthSummary.setText("许可证校验失败，部分巡检功能可能受限。");
            tvSyncTip.setText("激活并同步云端许可证");
        }
    }

    private void refreshLicense() {
        String licenseKey = licenseManager.getLicenseKey();
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            Toast.makeText(this, "本机没有许可证密钥，请先在登录页激活", Toast.LENGTH_SHORT).show();
            return;
        }

        LicenseApiService apiService = AuthApiConfig.createLicenseService();
        apiService.refresh(new LicenseRequest(licenseKey, licenseManager.getMachineCode()))
                .enqueue(new Callback<LicenseResponse>() {
                    @Override
                    public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(AuthInfoActivity.this, "刷新失败: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        LicenseResponse body = response.body();
                        if (body.getCode() == 200 && licenseManager.saveLicense(body.getData())) {
                            Toast.makeText(AuthInfoActivity.this, "云端许可证已成功同步并刷新", Toast.LENGTH_SHORT).show();
                            renderLocalLicense();
                        } else {
                            Toast.makeText(AuthInfoActivity.this, body.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LicenseResponse> call, Throwable t) {
                        Toast.makeText(AuthInfoActivity.this, "连接服务器失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
