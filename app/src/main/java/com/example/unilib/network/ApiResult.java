package com.example.unilib.network;

public class ApiResult<T> {
    public enum Status { SUCCESS, NO_INTERNET, TIMEOUT, UNAUTHORIZED, HTTP_ERROR, PARSE_ERROR, UNKNOWN }

    public final Status status;
    public final T data;
    public final String message;

    private ApiResult(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResult<T> success(T data) { return new ApiResult<>(Status.SUCCESS, data, null); }
    public static <T> ApiResult<T> error(Status status, String message) { return new ApiResult<>(status, null, message); }
    public boolean isSuccess() { return status == Status.SUCCESS; }
}