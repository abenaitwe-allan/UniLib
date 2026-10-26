package com.example.unilib.network;

public interface ApiCallback<T> {
    void onResult(ApiResult<T> result);   // may be called on a background thread
}