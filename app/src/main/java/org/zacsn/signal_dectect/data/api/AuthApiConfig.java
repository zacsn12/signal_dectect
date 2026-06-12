package org.zacsn.signal_dectect.data.api;

import java.net.Proxy;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class AuthApiConfig {
    public static final String BASE_URL = "http://47.82.157.64:1234/";

    private AuthApiConfig() {
    }

    public static AuthApiService createService() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(AuthApiService.class);
    }
}
