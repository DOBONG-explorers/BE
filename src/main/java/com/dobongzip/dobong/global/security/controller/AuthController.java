package com.dobongzip.dobong.global.security.controller;

import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.CommonResponse;
import com.dobongzip.dobong.global.security.dto.auth.request.AppLoginRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.AppSignupRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.PasswordResetRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.request.ProfileRequestDto;
import com.dobongzip.dobong.global.security.dto.auth.response.LoginResponseDto;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import com.dobongzip.dobong.global.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "로그인과 회원가입")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedProvider authenticatedProvider;


    /**
     * 일반 로그인 (앱 사용자용)
     */
    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(
            @RequestBody @Valid AppLoginRequestDto request
    ) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }

    /**
     * 회원가입 (앱 사용자용)
     */
    @Operation(summary = "앱 회원가입", description = "이메일, 비밀번호, 휴대폰 번호로 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<LoginResponseDto>> signup(@RequestBody @Valid AppSignupRequestDto request) {
        LoginResponseDto response = authService.signup(request);
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }


    /**
     * 프로필 등록 (회원가입 2단계)
     */
    @Operation(summary = "앱 회원가입 2단계 - 프로필 입력", description = "이름, 닉네임, 성별, 생일 정보를 등록합니다.")
    @PostMapping("/profile")
    public ResponseEntity<CommonResponse<String>> completeProfile(
            @RequestBody @Valid ProfileRequestDto request,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LoginType loginType
    ) {
        if (authenticatedProvider.isAuthenticated()) {
            // ✅ 소셜 로그인
            User user = authenticatedProvider.getCurrentUser();
            authService.updateProfile(user.getEmail(), user.getLoginType(), request);
        } else {
            // ✅ 앱 회원가입
            if (email == null || loginType == null) {
                throw new BusinessException(StatusCode.INVALID_REQUEST);
            }
            authService.updateProfile(email, loginType, request);
        }

        return ResponseEntity.ok(CommonResponse.onSuccess("프로필 등록 완료"));
    }

    @Operation(summary = "앱 비밀번호 찾기", description = "이메일로 사용자를 찾고 비밀번호를 재설정합니다.")
    @PostMapping("/password/reset")
    public ResponseEntity<CommonResponse<String>> resetPassword(@RequestBody PasswordResetRequestDto dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok(CommonResponse.onSuccess("비밀번호가 재설정되었습니다."));
    }

}
