package com.dobongzip.dobong.global.security.controller;

import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.CommonResponse;
import com.dobongzip.dobong.global.security.dto.auth.request.*;
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

    @Operation(
            summary = "카카오 로그인 (OIDC)",
            description = """
    안드로이드에서 Kakao OIDC로 받은 **ID 토큰(id_token)** 을 서버에 전달해 로그인/자동가입을 수행합니다.
    
    ## 프론트 가이드
    - **반드시 `id_token`** 을 보내세요. (access_token 아님)
    - 로그인 요청 시 클라이언트는 Kakao SDK에서 **scope에 `openid` + `account_email`** 포함.
    - 서버 응답의 `token`은 **우리 서비스 JWT** 입니다. 이후 API 호출 시
      `Authorization: Bearer {token}` 로 전송하세요.
    - `profileCompleted`가 **false**면 앱 회원가입 2단계(닉네임/이름/프사 등) 입력 화면으로 이동 후
      `/auth/profile` 에 저장하세요.
    
    
    ## 보안/검증 요약(서버 측)
    - Kakao OIDC의 `iss/aud/exp(/nonce)` 검증을 수행합니다.
    - 이메일은 **필수 동의**여야 하며, 미제공 시 가입/로그인을 거부합니다.
    - 신규 사용자일 경우 email만으로 계정 생성하고 `profileCompleted=false` 로 반환합니다.
    
    ## 정상 응답 예시(HTTP 200)
    {
      "isSuccess": true,
      "code": "OK",
      "message": "success",
      "result": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",   // 우리 서비스 JWT
        "profileCompleted": false,
        "name": null,
        "nickname": null,
        "loginType": "KAKAO"
      }
    }
    """

    )    @PostMapping("/kakao/oidc")
    public ResponseEntity<CommonResponse<LoginResponseDto>> kakaoOidc(@RequestBody OIDCRequestDto request) {
        LoginResponseDto response = authService.loginWithKakaoIdToken(request.getIdToken());
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }


    @Operation(
            summary = "구글 로그인 (OIDC)",
            description = """
    Android에서 Google Identity Services로 받은 **ID 토큰(id_token)** 을 서버로 보내 로그인/자동가입을 수행합니다.

    - 클라: `serverClientId`에 **웹 클라이언트 ID**를 사용하여 id_token을 발급받을 것.
    - 가능하면 `nonce`도 함께 보내 재생공격을 방지합니다.
    - 응답의 `token`은 **우리 서비스 JWT**입니다. 이후 `Authorization: Bearer {token}`로 호출하세요.
    - `profileCompleted == false`면 `/auth/profile` 2단계 가입을 완료하세요.
    """
    )
    @PostMapping("/google/oidc")
    public ResponseEntity<CommonResponse<LoginResponseDto>> googleOidc(
            @RequestBody @Valid OIDCRequestDto request
    ) {
        // AuthService에 loginWithGoogleIdToken(idToken) 메서드가 있다고 가정
        LoginResponseDto response = authService.loginWithGoogleIdToken(request.getIdToken());
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
            // 소셜 로그인
            User user = authenticatedProvider.getCurrentUser();
            authService.updateProfile(user.getEmail(), user.getLoginType(), request);
        } else {
            // 앱 회원가입
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
