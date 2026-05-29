package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.LoginResponse;
import org.zacsn.signal_dectect.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthInfoActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvValidUntil;
    private TextView tvAuthStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_info);

        ImageView ivBack = findViewById(R.id.iv_back);
        TextView tvSerialNumber = findViewById(R.id.tv_serial_number);
        tvValidUntil = findViewById(R.id.tv_valid_until);
        tvAuthStatus = findViewById(R.id.tv_auth_status);

        ivBack.setOnClickListener(v -> finish());
        
        sessionManager = new SessionManager(this);
        
        tvSerialNumber.setText(sessionManager.getSerialNumber());
        tvValidUntil.setText(sessionManager.getValidUntil());
        refreshAuthorizationInfo();
    }

    private void refreshAuthorizationInfo() {
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.10.8:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AuthApiService apiService = retrofit.create(AuthApiService.class);
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
                            data.getValidUntil()
                    );
                    tvValidUntil.setText(data.getValidUntil());
                }

                if (loginResponse.getCode() == 200) {
                    tvAuthStatus.setText("已授权");
                    tvAuthStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                } else if (loginResponse.getCode() == 403) {
                    tvAuthStatus.setText("已过期");
                    tvAuthStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                    Toast.makeText(AuthInfoActivity.this, loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AuthInfoActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(AuthInfoActivity.this, "授权信息同步失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
