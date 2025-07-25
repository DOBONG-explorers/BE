package com.dobongzip.dobong.domain.user.controller;

import com.dobongzip.dobong.domain.user.service.UserService;
import com.dobongzip.dobong.global.response.CommonResponse;
import com.dobongzip.dobong.global.security.dto.request.SignupRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@Tag(name = "회원가입")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "회원가입 정보를 입력하여 사용자 계정을 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "SUCCESS"),
                    @ApiResponse(responseCode = "400", description = "중복된 아이디 또는 비밀번호 형식 오류")
            }
    )
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDto request) {
        userService.signup(request);
        return ResponseEntity.ok(CommonResponse.onSuccess());
    }
}
