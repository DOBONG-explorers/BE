package com.dobongzip.dobong.global.security.dto.request;

import lombok.Getter;

@Getter
public class LoginRequestDto {
    private String appId;
    private String password;
}