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
        
        org.zacsn.signal_dectect.util.SessionManager sessionManager = new org.zacsn.signal_dectect.util.SessionManager(requireContext());
        
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
        
        setupRow(view, R.id.row_password, R.drawable.ic_password, "密码修改", null, v -> {
            startActivity(new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.ChangePasswordActivity.class));
        });
        
        // Logout Row
        setupRow(view, R.id.row_logout, R.drawable.ic_logout, "退出登录", null, v -> {
            sessionManager.logout();
            android.content.Intent intent = new android.content.Intent(requireContext(), org.zacsn.signal_dectect.presentation.activity.LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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
