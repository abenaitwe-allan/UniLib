package com.example.unilib.network;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

public final class ErrorClassifier {
    private ErrorClassifier() { }

    public static <T> ApiResult<T> fromException(IOException e) {
        // SocketTimeoutException is a subclass of InterruptedIOException
        if (e instanceof InterruptedIOException) {
            return ApiResult.error(ApiResult.Status.TIMEOUT, "The request took too long. Check your connection and try again.");
        }
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return ApiResult.error(ApiResult.Status.NO_INTERNET, "No internet connection.");
        }
        return ApiResult.error(ApiResult.Status.UNKNOWN, "Network problem. Please try again.");
    }

    public static <T> ApiResult<T> fromHttpCode(int code) {
        if (code == 401 || code == 403) {
            return ApiResult.error(ApiResult.Status.UNAUTHORIZED, "Your session expired or you do not have permission. Please log in again.");
        }
        if (code == 429) {
            return ApiResult.error(ApiResult.Status.HTTP_ERROR, "Too many requests. Wait a moment and try again.");
        }
        if (code >= 500) {
            return ApiResult.error(ApiResult.Status.HTTP_ERROR, "The server has a problem. Try again later.");
        }
        return ApiResult.error(ApiResult.Status.HTTP_ERROR, "Request failed (code " + code + ").");
    }
}