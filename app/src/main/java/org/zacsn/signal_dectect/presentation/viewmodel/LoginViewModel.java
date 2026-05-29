package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.LoginRequest;
import org.zacsn.signal_dectect.data.api.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoginSuccess = new MutableLiveData<>();
    private LoginResponse.Data loginData;

    private AuthApiService apiService;

    public LoginViewModel() {
        // LAN development server. Keep the trailing slash for Retrofit.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.10.8:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AuthApiService.class);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getLoginResult() { return loginResult; }
    public LiveData<Boolean> getIsLoginSuccess() { return isLoginSuccess; }
    public LoginResponse.Data getLoginData() { return loginData; }

    public void login(String username, String password, boolean isTestMode, String expectedPassword) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginResult.setValue("用户名或密码不能为空");
            isLoginSuccess.setValue(false);
            loginData = null;
            return;
        }

        isLoading.setValue(true);

        if (isTestMode) {
            // Mock Login Logic
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Simulate network delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isLoading.postValue(false);
                if ("admin".equals(username) && expectedPassword.equals(password)) {
                    loginData = null;
                    isLoginSuccess.postValue(true);
                    loginResult.postValue("登录成功 (测试环境)");
                } else {
                    loginData = null;
                    isLoginSuccess.postValue(false);
                    loginResult.postValue("账号或密码错误");
                }
            }).start();
        } else {
            // Real Network Login
            LoginRequest request = new LoginRequest(username, password);
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
                        loginResult.setValue("服务器异常: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    isLoading.setValue(false);
                    loginData = null;
                    isLoginSuccess.setValue(false);
                    loginResult.setValue("网络请求失败: " + t.getMessage());
                }
            });
        }
    }
}
