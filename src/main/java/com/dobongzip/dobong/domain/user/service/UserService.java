package com.dobongzip.dobong.domain.user.service;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.dto.request.SignupRequestDto;
import com.dobongzip.dobong.global.security.enums.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void signup(SignupRequestDto request) {

        // 비밀번호 조건 검증
        if (!isValidPassword(request.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_PASSWORD_FORMAT);
        }

        // User 생성
        User user = User.builder()
                .appId(request.getAppId())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .name(request.getName())
                .email(request.getEmail())
                .gender(request.getGender())
                .birth(request.getBirth())
                .age(request.getAge())
                .loginType(LoginType.APP)
                .build();

        userRepository.save(user);
    }


    private boolean isValidPassword(String password) {
        if (password.length() < 6 || password.length() > 20) return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        int count = 0;
        if (hasUpper) count++;
        if (hasLower) count++;
        if (hasSpecial) count++;

        return count >= 2;
    }

}
