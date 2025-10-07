package com.dobongzip.dobong.domain.mypage.controller;

import com.dobongzip.dobong.domain.mypage.dto.request.PasswordChangeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.ProfileManageDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ProfileResponseDto;
import com.dobongzip.dobong.domain.mypage.service.MypageService;
import com.dobongzip.dobong.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지")
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService myPageService;

    @Operation(summary = "개인정보 조회", description = "닉네임, 생년월일, 이메일을 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<CommonResponse<ProfileResponseDto>> getProfile() {
        ProfileResponseDto response = myPageService.getProfile();
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }


    @Operation(summary = "개인정보 수정", description = "닉네임, 생일, 이메일을 수정합니다.")
    @PatchMapping("/profile")
    public ResponseEntity<CommonResponse<String>> updateProfile(@RequestBody ProfileManageDto dto) {
        myPageService.updateProfile(dto);
        return ResponseEntity.ok(CommonResponse.onSuccess("개인정보 수정 완료"));
    }


    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다.")
    @PatchMapping("/password")
    public ResponseEntity<CommonResponse<String>> changePassword(@RequestBody PasswordChangeRequestDto dto) {
        myPageService.changePassword(dto);
        return ResponseEntity.ok(CommonResponse.onSuccess("비밀번호 변경 완료"));
    }
}
