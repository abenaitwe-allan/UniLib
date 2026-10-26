package com.example.unilib.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public final class HttpClientProvider {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)      // hard limit for the whole request
            .build();

    private HttpClientProvider() { }

    public static OkHttpClient get() { return CLIENT; }

    /** PDFs up to 10 MB need longer than a normal request. */
    public static OkHttpClient forUpload() {
        return CLIENT.newBuilder().callTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(60, TimeUnit.SECONDS).build();
    }
}