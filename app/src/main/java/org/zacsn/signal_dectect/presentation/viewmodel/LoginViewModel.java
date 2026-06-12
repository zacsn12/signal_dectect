package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.LoginRequest;
import org.zacsn.signal_dectect.data.api.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoginSuccess = new MutableLiveData<>();
    private LoginResponse.Data loginData;

    private AuthApiService apiService;

    public LoginViewModel() {
        apiService = AuthApiConfig.createService();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getLoginResult() { return loginResult; }
    public LiveData<Boolean> getIsLoginSuccess() { return isLoginSuccess; }
    public LoginResponse.Data getLoginData() { return loginData; }

    public void login(String username, String password, String machineCode) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginResult.setValue("用户名或密码不能为空");
            isLoginSuccess.setValue(false);
            loginData = null;
            return;
        }
        if (machineCode == null || machineCode.trim().isEmpty()) {
            loginResult.setValue("机器码生成失败，请重新打开 App 后再试");
            isLoginSuccess.setValue(false);
            loginData = null;
            return;
        }

        isLoading.setValue(true);

        LoginRequest request = new LoginRequest(username, password, machineCode);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.getCode() == 200) {
                        loginData = loginResponse.getData();
                        isLoginSuccess.setValue(true);
                        loginResult.setValue("登录成功");
                    } else {
                        loginData = null;
                        isLoginSuccess.setValue(false);
                        loginResult.setValue(loginResponse.getMessage());
                    }
            } else {
                loginData = null;
                isLoginSuccess.setValue(false);
                loginResult.setValue(
                        "服务器响应异常\n"
                                + "接口地址: " + AuthApiConfig.BASE_URL + "api/auth/login\n"
                                + "HTTP状态码: " + response.code()
                );
            }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading.setValue(false);
                loginData = null;
                isLoginSuccess.setValue(false);
                loginResult.setValue(
                        "网络请求失败\n"
                                + "接口地址: " + AuthApiConfig.BASE_URL + "api/auth/login\n"
                                + "错误类型: " + t.getClass().getName() + "\n"
                                + "错误信息: " + (t.getMessage() != null ? t.getMessage() : "无") + "\n\n"
                                + "如错误信息出现 192.168.*:9000，通常是手机系统代理导致。当前版本已配置认证接口直连服务器。"
                );
            }
        });
    }
}
