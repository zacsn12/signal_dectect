package org.zacsn.signal_dectect.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LicenseApiService {
    @POST("/api/license/activate")
    Call<LicenseResponse> activate(@Body LicenseRequest request);

    @POST("/api/license/refresh")
    Call<LicenseResponse> refresh(@Body LicenseRequest request);
}
