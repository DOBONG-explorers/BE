package com.dobongzip.dobong.global.security.util;

import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;

public class PasswordValidator {

    public static void validate(String password) {
        // 비밀번호 길이 체크
        if (password.length() < 6 || password.length() > 20) {
            throw new BusinessException(StatusCode.INVALID_PASSWORD_FORMAT);
        }

        // 구성요소 체크 (대소문자/특수문자)
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        int count = 0;
        if (hasLower) count++;
        if (hasUpper) count++;
        if (hasSpecial) count++;

        if (count < 2) {
            throw new BusinessException(StatusCode.INVALID_PASSWORD_FORMAT);
        }
    }
}
