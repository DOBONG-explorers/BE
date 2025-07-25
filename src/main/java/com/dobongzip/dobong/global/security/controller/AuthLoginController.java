package com.dobongzip.dobong.global.security.controller;

import com.dobongzip.dobong.global.response.CommonResponse;
import com.dobongzip.dobong.global.security.dto.request.LoginRequestDto;
import com.dobongzip.dobong.global.security.dto.response.LoginResponseDto;
import com.dobongzip.dobong.global.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "로그인")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthLoginController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호를 입력하여 JWT 토큰을 발급받습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "SUCCESS"),
                    @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 일치하지 않음")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }
}
