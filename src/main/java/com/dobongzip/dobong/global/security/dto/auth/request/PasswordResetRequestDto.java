package com.dobongzip.dobong.global.security.dto.auth.request;

import lombok.Getter;

@Getter
public class PasswordResetRequestDto {
    private String email;
    private String newPassword;
    private String confirmPassword;
}

