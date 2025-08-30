package com.dobongzip.dobong.global.security.dto.auth.response;

import com.dobongzip.dobong.global.security.enums.LoginType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private boolean isProfileCompleted; // 🔄 소셜/일반 로그인 공통 판단 용도

    private String name;
    private String nickname;
    private LoginType loginType;
}