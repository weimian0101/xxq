package com.example.gdms.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static ApiResponse<?> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<?> fail(String msg) {
        return new ApiResponse<>(false, msg, null);
    }
}

