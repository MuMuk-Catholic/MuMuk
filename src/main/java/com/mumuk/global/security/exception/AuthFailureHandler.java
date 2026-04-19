package com.mumuk.global.security.exception;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.Response;
import lombok.Getter;

@Getter
public class AuthFailureHandler extends RuntimeException {
    private final ErrorCode errorCode;

    public AuthFailureHandler(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public Response<String> toResponse() {
        return new Response<>(false, errorCode.getCode(), errorCode.getMessage());
    }
}
