package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.LicenseApiService;
import org.zacsn.signal_dectect.data.api.LicenseRequest;
import org.zacsn.signal_dectect.data.api.LicenseResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoginSuccess = new MutableLiveData<>();
    private LicenseResponse.Data licenseData;

    private LicenseApiService apiService;

    public LoginViewModel() {
        apiService = AuthApiConfig.createLicenseService();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getLoginResult() { return loginResult; }
    public LiveData<Boolean> getIsLoginSuccess() { return isLoginSuccess; }
    public LicenseResponse.Data getLicenseData() { return licenseData; }

    public void login(String username, String password, String machineCode) {
        activateLicense(username, machineCode);
    }

    public void activateLicense(String licenseKey, String machineCode) {
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            loginResult.setValue("许可证不能为空");
            isLoginSuccess.setValue(false);
            licenseData = null;
            return;
        }
        if (machineCode == null || machineCode.trim().isEmpty()) {
            loginResult.setValue("机器码生成失败，请重新打开 App 后再试");
            isLoginSuccess.setValue(false);
            licenseData = null;
            return;
        }

        isLoading.setValue(true);

        LicenseRequest request = new LicenseRequest(licenseKey, machineCode);
        apiService.activate(request).enqueue(new Callback<LicenseResponse>() {
            @Override
            public void onResponse(Call<LicenseResponse> call, Response<LicenseResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LicenseResponse licenseResponse = response.body();
                    if (licenseResponse.getCode() == 200) {
                        licenseData = licenseResponse.getData();
                        isLoginSuccess.setValue(true);
                        loginResult.setValue("许可证激活成功");
                    } else {
                        licenseData = null;
                        isLoginSuccess.setValue(false);
                        loginResult.setValue(licenseResponse.getMessage());
                    }
            } else {
                licenseData = null;
                isLoginSuccess.setValue(false);
                loginResult.setValue(
                        "服务器响应异常\n"
                                + "接口地址: " + AuthApiConfig.BASE_URL + "api/license/activate\n"
                                + "HTTP状态码: " + response.code()
                );
            }
            }

            @Override
            public void onFailure(Call<LicenseResponse> call, Throwable t) {
                isLoading.setValue(false);
                licenseData = null;
                isLoginSuccess.setValue(false);
                loginResult.setValue(
                        "网络请求失败\n"
                                + "接口地址: " + AuthApiConfig.BASE_URL + "api/license/activate\n"
                                + "错误类型: " + t.getClass().getName() + "\n"
                                + "错误信息: " + (t.getMessage() != null ? t.getMessage() : "无")
                );
            }
        });
    }
}
