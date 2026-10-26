package com.example.unilib.data;

public interface ResultCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
