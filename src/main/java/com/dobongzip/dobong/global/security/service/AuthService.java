package com.dobongzip.dobong.global.security.service;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.s3.service.ImageService;
import com.dobongzip.dobong.global.security.dto.auth.request.AppLoginRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.AppSignupRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.PasswordResetRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.ProfileRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.response.LoginResponseDto;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import com.dobongzip.dobong.global.security.util.JwtUtil;
import com.dobongzip.dobong.global.security.util.PasswordValidator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KakaoOidcService kakaoOidcService;
    private final GoogleOidcService googleOidcService;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtExpiryService jwtExpiryService;
    private final AuthenticatedProvider authenticatedProvider;
    private final ImageService imageService;

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
        PasswordValidator.validate(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .loginType(LoginType.APP)
                .profileCompleted(false)
                .build();

        userRepository.save(user);

        // 토큰 발급
        String token = jwtUtil.createAccessToken(user.getEmail(), user.getLoginType().name());

        return new LoginResponseDto(token, false, null, null, user.getLoginType());
    }


    // 2단계 프로필 입력
    @Transactional
    public void updateProfile(String email, LoginType loginType, ProfileRequestDto request) {
        User user = userRepository.findByEmailAndLoginType(email, loginType)
                .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));

        user.updateProfile(request);
    }


    @Transactional
    public void resetPassword(PasswordResetRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND_BY_EMAIL));

        //  로그인 타입이 APP인지 확인
        if (user.getLoginType() != LoginType.APP) {
            throw new BusinessException(StatusCode.NOT_ALLOWED_FOR_SOCIAL_LOGIN);
        }

        // 비밀번호 일치 & 포맷 검증
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(StatusCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        PasswordValidator.validate(dto.getNewPassword());

        // 새 비밀번호 저장
        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }


    /** 카카오 OIDC id_token 로그인 (자동 가입 포함) */
    @Transactional
    public LoginResponseDto loginWithKakaoIdToken(String idToken) {
        var claims = kakaoOidcService.verify(idToken);

        // 이메일이 필수 (User.email unique이기 때문)
        if (claims.email() == null || claims.email().isBlank()) {
            throw new BusinessException(StatusCode.EMAIL_NOT_PROVIDED);
        }

        // 이미 존재? -> 그대로 사용 (loginType은 이후 프로필에서 설정/혹은 여기서 설정)
        User user = userRepository.findByEmail(claims.email())
                .orElseGet(() -> {
                    // 신규 생성: email만으로 생성하고, loginType=KAKAO로 설정
                    User newbie = User.builder()
                            .email(claims.email())
                            .loginType(LoginType.KAKAO)
                            .profileCompleted(false)
                            .build();
                    return userRepository.save(newbie);
                });
        String token = jwtUtil.createAccessToken(user.getEmail(), LoginType.KAKAO.name());
        return new LoginResponseDto(token, user.isProfileCompleted(), user.getName(), user.getNickname(), LoginType.KAKAO);
    }


    @Transactional
    public LoginResponseDto loginWithGoogleIdToken(String idToken) {
        var claims = googleOidcService.verify(idToken);

        if (claims.email() == null || claims.email().isBlank()) {
            throw new BusinessException(StatusCode.EMAIL_NOT_PROVIDED);
        }

        User user = userRepository.findByEmail(claims.email())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(claims.email())
                                .loginType(LoginType.GOOGLE)
                                .profileCompleted(false) // 닉네임/이름/프사는 2단계에서 입력
                                .build()
                ));

        String token = jwtUtil.createAccessToken(user.getEmail(), LoginType.GOOGLE.name());
        return new LoginResponseDto(token, user.isProfileCompleted(), user.getName(), user.getNickname(), LoginType.GOOGLE);
    }


    /** 로그아웃: Access 토큰 블랙리스트 등록 */
    @Transactional
    public void logout(String authorizationHeader, HttpServletResponse response) {
        // 로그인 사용자 존재 보장
        authenticatedProvider.getCurrentUser();

        // 1) Access 토큰 파싱
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7).trim(); // ★ trim
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                var exp = jwtExpiryService.getExpiration(accessToken);
                var ttl = java.time.Duration.between(java.time.Instant.now(), exp);
                // ★ ttl 최소 1초 보정은 JwtBlacklistService에서 최종 보정
                jwtBlacklistService.block(accessToken, ttl);
                System.out.println("[LOGOUT] blacklisted suffix="
                        + accessToken.substring(Math.max(0, accessToken.length()-16))
                        + " ttl=" + Math.max(1, ttl.getSeconds()) + "s");
            } catch (Exception e) {
                // ★ 파싱 실패(만료/시크릿 mismatch)여도 5분 차단 — 동작 점검용
                jwtBlacklistService.block(accessToken, java.time.Duration.ofMinutes(5));
                System.out.println("[LOGOUT] parse fail -> fallback 5m; suffix="
                        + accessToken.substring(Math.max(0, accessToken.length()-16))
                        + " err=" + e.getMessage());
            }
        }
    }

    /** 회원탈퇴(소프트 삭제): 비번 재확인 없이 처리 */
    @Transactional
    public void withdrawSoft() {
        User user = authenticatedProvider.getCurrentUser();

        // 프로필 이미지 정리
        imageService.removeProfileImage(user);

        // 이메일/닉네임 치환(유니크 충돌 방지)
        user.setNickname("탈퇴회원");
        user.setEmail(user.getId() + "+deleted@" + user.getLoginType().name().toLowerCase() + ".invalid");

        // 소프트 삭제 마킹 (active=false, deletedAt=now)
        user.softDelete();
    }
}
