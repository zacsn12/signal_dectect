package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.LoginResponse;
import org.zacsn.signal_dectect.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class AuthInfoActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvLocalMachineCode;
    private TextView tvAuthSummary;
    private TextView tvBindingCapacity;
    private TextView tvLocalBindingStatus;
    private TextView tvBoundMachines;
    private TextView tvValidUntil;
    private TextView tvAuthStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_info);

        ImageView ivBack = findViewById(R.id.iv_back);
        tvLocalMachineCode = findViewById(R.id.tv_local_machine_code);
        tvAuthSummary = findViewById(R.id.tv_auth_summary);
        tvBindingCapacity = findViewById(R.id.tv_binding_capacity);
        tvLocalBindingStatus = findViewById(R.id.tv_local_binding_status);
        tvBoundMachines = findViewById(R.id.tv_bound_machines);
        tvValidUntil = findViewById(R.id.tv_valid_until);
        tvAuthStatus = findViewById(R.id.tv_auth_status);

        ivBack.setOnClickListener(v -> finish());
        
        sessionManager = new SessionManager(this);
        
        tvLocalMachineCode.setText(sessionManager.getMachineCode());
        tvValidUntil.setText(sessionManager.getValidUntil());
        tvBindingCapacity.setText("-/" + sessionManager.getMaxMachineBindings() + " 台");
        tvLocalBindingStatus.setText("等待服务端同步");
        refreshAuthorizationInfo();
    }

    private void refreshAuthorizationInfo() {
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        AuthApiService apiService = AuthApiConfig.createService();
        apiService.currentUser("Bearer " + token).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AuthInfoActivity.this, "授权信息同步失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse loginResponse = response.body();
                LoginResponse.Data data = loginResponse.getData();
                if (data != null && data.getValidUntil() != null) {
                    sessionManager.updateAuthorizationInfo(
                            data.getUserId(),
                            data.getNickname(),
                            data.getValidUntil(),
                            data.getMachineCode(),
                            data.getMaxMachineBindings()
                    );
                    tvValidUntil.setText(data.getValidUntil());
                    renderBindingInfo(data);
                }

                if (loginResponse.getCode() == 200) {
                    tvAuthStatus.setText("已授权");
                    tvAuthStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                    tvAuthSummary.setText("当前账号授权有效，绑定设备在容量范围内可正常使用");
                } else if (loginResponse.getCode() == 403) {
                    tvAuthStatus.setText("已过期");
                    tvAuthStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                    tvAuthSummary.setText(loginResponse.getMessage());
                    Toast.makeText(AuthInfoActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AuthInfoActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(AuthInfoActivity.this, "授权信息同步失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                tvAuthSummary.setText("授权信息同步失败，请检查网络后重新进入本页面");
            }
        });
    }

    private void renderBindingInfo(LoginResponse.Data data) {
        int maxBindings = Math.max(1, data.getMaxMachineBindings());
        int boundCount = data.getMachineBindings() != null ? data.getMachineBindings().size() : 0;
        String localMachineCode = sessionManager.getMachineCode();
        String currentMachineCode = data.getMachineCode();

        tvBindingCapacity.setText(boundCount + "/" + maxBindings + " 台");
        tvLocalMachineCode.setText(localMachineCode);

        boolean localBound = false;
        StringBuilder boundMachinesText = new StringBuilder();
        if (data.getMachineBindings() != null && !data.getMachineBindings().isEmpty()) {
            for (int i = 0; i < data.getMachineBindings().size(); i++) {
                LoginResponse.MachineBinding binding = data.getMachineBindings().get(i);
                String machineCode = binding.getMachineCode() != null ? binding.getMachineCode() : "";
                if (machineCode.equalsIgnoreCase(localMachineCode)
                        || machineCode.equalsIgnoreCase(currentMachineCode)) {
                    localBound = true;
                }
                boundMachinesText
                        .append(i + 1)
                        .append(". ")
                        .append(machineCode.isEmpty() ? "未知机器码" : machineCode);
                if (binding.getBoundAt() != null && !binding.getBoundAt().trim().isEmpty()) {
                    boundMachinesText.append("\n   绑定时间: ").append(binding.getBoundAt());
                }
                if (i < data.getMachineBindings().size() - 1) {
                    boundMachinesText.append("\n\n");
                }
            }
        }

        if (boundMachinesText.length() == 0) {
            tvBoundMachines.setText("暂无绑定记录");
        } else {
            tvBoundMachines.setText(boundMachinesText.toString());
        }

        if (localBound) {
            tvLocalBindingStatus.setText("本机已绑定，可使用当前账号");
            tvLocalBindingStatus.setTextColor(getColor(android.R.color.holo_green_dark));
        } else if (boundCount < maxBindings) {
            tvLocalBindingStatus.setText("本机尚未绑定，重新登录成功后会占用 1 个绑定名额");
            tvLocalBindingStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            tvLocalBindingStatus.setText("本机未绑定，且账号绑定名额已满，请联系管理员调整");
            tvLocalBindingStatus.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }
}
