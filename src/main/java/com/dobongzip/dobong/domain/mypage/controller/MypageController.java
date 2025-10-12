package com.dobongzip.dobong.domain.mypage.controller;

import com.dobongzip.dobong.domain.mypage.dto.request.ImageFinalizeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.PasswordChangeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.ProfileManageDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ImageObjectKeyResponseDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ImageUrlResponseDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ProfileResponseDto;
import com.dobongzip.dobong.domain.mypage.service.MypageService;
import com.dobongzip.dobong.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "마이페이지")
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService myPageService;

    @Operation(summary = "개인정보 조회", description = "닉네임, 생년월일, 이메일을 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<CommonResponse<ProfileResponseDto>> getProfile() {
        ProfileResponseDto response = myPageService.getProfile();
        return ResponseEntity.ok(CommonResponse.onSuccess(response));
    }

    @Operation(summary = "프로필 이미지 조회")
    @GetMapping("/profile-image")
    public ResponseEntity<CommonResponse<ImageUrlResponseDto>> getProfileImage() {
        return ResponseEntity.ok(CommonResponse.onSuccess(myPageService.getProfileImage()));
    }
    @Operation(summary = "프로필 이미지 업로드 (PNG만 허용) → objectKey만 반환")
    @PostMapping(value = "/profile-image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ImageObjectKeyResponseDto>> uploadProfileImage(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(CommonResponse.onSuccess(myPageService.uploadProfileImage(file)));
    }

    @Operation(summary = "프로필 이미지 최종 반영")
    @PostMapping("/profile-image/finalize")
    public ResponseEntity<CommonResponse<ImageUrlResponseDto>> finalizeProfileImage(
            @RequestBody ImageFinalizeRequestDto dto) {
        return ResponseEntity.ok(CommonResponse.onSuccess(myPageService.finalizeProfileImage(dto)));
    }

    @Operation(summary = "프로필 이미지 제거 (기본 이미지로 복귀)")
    @DeleteMapping("/profile-image")
    public ResponseEntity<CommonResponse<String>> removeProfileImage() {
        myPageService.removeProfileImage();
        return ResponseEntity.ok(CommonResponse.onSuccess("프로필 이미지 삭제 완료"));
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
