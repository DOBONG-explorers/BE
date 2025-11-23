package com.dobongzip.dobong.domain.mypage.service;

import com.dobongzip.dobong.domain.mypage.dto.request.ImageFinalizeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.PasswordChangeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.ProfileManageDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ImageObjectKeyResponseDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ImageUrlResponseDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ProfileResponseDto;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.s3.service.ImageService;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import com.dobongzip.dobong.global.security.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final AuthenticatedProvider authenticatedProvider;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile() {
        User user = authenticatedProvider.getCurrentUser();
        return new ProfileResponseDto(
                user.getNickname(),
                user.getBirth(),
                user.getEmail()
        );
    }

    @Transactional
    public void updateProfile(ProfileManageDto dto) {
        User user = authenticatedProvider.getCurrentUser();

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getBirth() != null) {
            user.setBirth(dto.getBirth());
        }
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            boolean exists = userRepository.existsByEmailAndLoginType(dto.getEmail(), user.getLoginType());
            if (exists) {
                throw new BusinessException(StatusCode.USER_ALREADY_EXISTS);
            }
            user.setEmail(dto.getEmail());
        }
    }

    /** 프로필 이미지 최종 반영 */
    @Transactional
    public ImageUrlResponseDto finalizeProfileImage(ImageFinalizeRequestDto dto) {
        User user = authenticatedProvider.getCurrentUser();
        var result = imageService.finalizeProfileImage(user, dto.getObjectKey());
        return new ImageUrlResponseDto(result.getUrl());
    }

    /** 프로필 이미지 제거 */
    @Transactional
    public void removeProfileImage() {
        User user = authenticatedProvider.getCurrentUser();
        imageService.removeProfileImage(user);
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePassword(PasswordChangeRequestDto dto) {
        User user = authenticatedProvider.getCurrentUser();

        if (user.getLoginType() != LoginType.APP) {
            throw new BusinessException(StatusCode.NOT_ALLOWED_FOR_SOCIAL_LOGIN);
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_CURRENT_PASSWORD);
        }
        PasswordValidator.validate(dto.getNewPassword());
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(StatusCode.PASSWORD_CONFIRM_NOT_MATCH);
        }
        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    /** 프로필 이미지 조회 (S3 URL 또는 기본 이미지 URL 반환) */
    @Transactional(readOnly = true)
    public ImageUrlResponseDto getProfileImage() {
        User me = authenticatedProvider.getCurrentUser();
        User user = userRepository.findById(me.getId()).orElseThrow(); // 영속화 보장
        String url = imageService.resolveUrl(user.getProfileImageKey());
        return new ImageUrlResponseDto(url);
    }

    @Transactional
    public ImageObjectKeyResponseDto uploadProfileImage(MultipartFile file) {
        User user = authenticatedProvider.getCurrentUser();

        // 1) PNG만 허용: Content-Type & 확장자 모두 체크 (이중 방어)
        String ct = file.getContentType();
        String name = file.getOriginalFilename();
        if (ct == null || !ct.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
            throw new BusinessException(StatusCode.UNSUPPORTED_MEDIA_TYPE); // 또는 커스텀 코드
        }
        if (name == null || !name.toLowerCase().endsWith(".png")) {
            throw new BusinessException(StatusCode.UNSUPPORTED_MEDIA_TYPE);
        }

        // 2) tmp 키 생성: user/{id}/tmp/YYYY/MM/{uuid}.png
        String key = imageService.createUserTmpPngKey(user.getId());

        // 3) S3 업로드 (백엔드 → S3)
        imageService.putPngObject(key, file);

        // 4) objectKey만 반환
        return new ImageObjectKeyResponseDto(key);
    }
}
