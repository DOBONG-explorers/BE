package com.dobongzip.dobong.global.security.dto.response;

import com.dobongzip.dobong.global.security.enums.LoginType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String name;
    private LoginType loginType;
}