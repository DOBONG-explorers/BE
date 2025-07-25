package com.dobongzip.dobong.global.security.service;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.dto.request.LoginRequestDto;

import com.dobongzip.dobong.global.security.dto.response.LoginResponseDto;
import com.dobongzip.dobong.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByAppId(request.getAppId())
                .orElseThrow(() -> new BusinessException(StatusCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.UNAUTHORIZED);
        }

        String token = jwtUtil.createAccessToken(user.getAppId());

        return new LoginResponseDto(token, user.getName(), user.getLoginType());
    }
}
