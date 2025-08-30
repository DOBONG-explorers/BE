package com.dobongzip.dobong.global.security.service;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.dto.auth.request.AppLoginRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.AppSignupRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.ProfileRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.response.LoginResponseDto;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;



    // 일반 로그인
    @Transactional(readOnly = true)
    public LoginResponseDto login(AppLoginRequestDto request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(request.getEmail()) && u.getLoginType() == LoginType.APP)
                .findFirst()
                .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_PASSWORD_FORMAT);
        }

        String token = jwtUtil.createAccessToken(user.getEmail(), user.getLoginType().name());
        return new LoginResponseDto(token, user.isProfileCompleted(), user.getName(), user.getNickname(), user.getLoginType());
    }

    // 일반 회원가입 1단계
    @Transactional
    public LoginResponseDto signup(AppSignupRequestDto request) {
        boolean exists = userRepository.existsByEmailAndLoginType(request.getEmail(), LoginType.APP);
        if (exists) {
            throw new BusinessException(StatusCode.USER_ALREADY_EXISTS);
        }
        validatePasswordFormat(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .loginType(LoginType.APP)
                .profileCompleted(false)
                .build();

        userRepository.save(user);

        // 토큰 발급
        String token = jwtUtil.createAccessToken(user.getEmail(),user.getLoginType().name());

        return new LoginResponseDto(token, false, null, null, user.getLoginType());
    }


    // 2단계 프로필 입력
    @Transactional
    public void updateProfile(String email, LoginType loginType, ProfileRequestDto request) {
        User user = userRepository.findByEmailAndLoginType(email, loginType)
                .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));

        user.updateProfile(request);
    }


    private void validatePasswordFormat(String password) {
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
