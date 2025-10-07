package com.dobongzip.dobong.global.exception;


import com.dobongzip.dobong.global.response.StatusCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final StatusCode statusCode;

    public CustomException(StatusCode statusCode) {
        super(statusCode.getDescription());
        this.statusCode = statusCode;
    }
}
