package com.dobongzip.dobong.global.security.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AppLoginRequestDto {
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}

