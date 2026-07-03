package org.zacsn.signal_dectect.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.zacsn.signal_dectect.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        org.zacsn.signal_dectect.util.LicenseManager licenseManager = new org.zacsn.signal_dectect.util.LicenseManager(requireContext());
        
        // Setup Header (we kept the username id for simplicity, but we'll set it to the app name per image, or username if you prefer)
        android.widget.TextView tvUsername = view.findViewById(R.id.tv_username);
        if (tvUsername != null) {
            tvUsername.setText("智能信号感知系统"); 
        }

        // Setup Rows
        setupRow(view, R.id.row_models, R.drawable.ic_device_model, "信号巡检机型", null, v -> {
            startActivity(new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.DeviceModelActivity.class));
        });
        
        setupRow(view, R.id.row_whitelist, R.drawable.ic_whitelist, "白名单管理", null, v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.class);
            intent.putExtra(org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.EXTRA_LIST_TYPE, org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.TYPE_WHITELIST);
            startActivity(intent);
        });
        
        setupRow(view, R.id.row_blacklist, R.drawable.ic_blacklist, "黑名单管理", null, v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.class);
            intent.putExtra(org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.EXTRA_LIST_TYPE, org.zacsn.signal_dectect.presentation.activity.ListManagerActivity.TYPE_BLACKLIST);
            startActivity(intent);
        });
        
        setupRow(view, R.id.row_auth, R.drawable.ic_auth, "授权信息", null, v -> {
            startActivity(new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.AuthInfoActivity.class));
        });
        
        setupRow(view, R.id.row_help, R.drawable.ic_help, "帮助中心", null, v -> {
            startActivity(new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.HelpCenterActivity.class));
        });
        
        setupRow(view, R.id.row_upgrade, R.drawable.ic_upgrade, "软件升级", "当前版本v" + org.zacsn.signal_dectect.BuildConfig.VERSION_NAME, v -> {
            startActivity(new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.UpgradeActivity.class));
        });
        
        setupRow(view, R.id.row_password, R.drawable.ic_password, "刷新授权", null, v -> {
            String licenseKey = licenseManager.getLicenseKey();
            if (licenseKey == null || licenseKey.trim().isEmpty()) {
                Toast.makeText(requireContext(), "本机暂无许可证密钥，请先在登录页激活", Toast.LENGTH_SHORT).show();
                return;
            }

            androidx.appcompat.app.AlertDialog progressDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            org.zacsn.signal_dectect.data.api.LicenseApiService apiService = 
                    org.zacsn.signal_dectect.data.api.AuthApiConfig.createLicenseService();
            apiService.refresh(new org.zacsn.signal_dectect.data.api.LicenseRequest(licenseKey, licenseManager.getMachineCode()))
                    .enqueue(new retrofit2.Callback<org.zacsn.signal_dectect.data.api.LicenseResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<org.zacsn.signal_dectect.data.api.LicenseResponse> call, 
                                               retrofit2.Response<org.zacsn.signal_dectect.data.api.LicenseResponse> response) {
                            progressDialog.dismiss();
                            if (!response.isSuccessful() || response.body() == null) {
                                Toast.makeText(requireContext(), "刷新失败: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            org.zacsn.signal_dectect.data.api.LicenseResponse body = response.body();
                            if (body.getCode() == 200 && licenseManager.saveLicense(body.getData())) {
                                Toast.makeText(requireContext(), "云端许可证已成功同步并刷新", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), body.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<org.zacsn.signal_dectect.data.api.LicenseResponse> call, Throwable t) {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "网络连接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        
        // Logout Row with double-confirmation dialog
        setupRow(view, R.id.row_logout, R.drawable.ic_logout, "清除许可证", null, v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("确认清除许可证？")
                    .setMessage("清除许可证后，本机的授权数据将被彻底抹除，巡检分析功能将失效。确定要清除并退出登录吗？")
                    .setPositiveButton("确认清除", (dialog, which) -> {
                        licenseManager.clearLicense();
                        Toast.makeText(requireContext(), "许可证已清除", Toast.LENGTH_SHORT).show();
                        android.content.Intent intent = new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.LoginActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void setupRow(View parentView, int rowId, int iconRes, String title, String value, View.OnClickListener listener) {
        View row = parentView.findViewById(rowId);
        if (row != null) {
            android.widget.ImageView ivIcon = row.findViewById(R.id.iv_icon);
            android.widget.TextView tvTitle = row.findViewById(R.id.tv_title);
            android.widget.TextView tvValue = row.findViewById(R.id.tv_value);

            if (ivIcon != null) ivIcon.setImageResource(iconRes);
            if (tvTitle != null) tvTitle.setText(title);
            
            if (tvValue != null) {
                if (value != null) {
                    tvValue.setText(value);
                    tvValue.setVisibility(View.VISIBLE);
                } else {
                    tvValue.setVisibility(View.GONE);
                }
            }
            
            if (listener != null) {
                row.setOnClickListener(listener);
            } else {
                row.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), title + "功能即将开放", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
