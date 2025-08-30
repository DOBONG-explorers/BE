package com.dobongzip.dobong.domain.mypage.service;

import com.dobongzip.dobong.domain.mypage.dto.request.PasswordChangeRequestDto;
import com.dobongzip.dobong.domain.mypage.dto.request.ProfileManageDto;
import com.dobongzip.dobong.domain.mypage.dto.response.ProfileResponseDto;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import com.dobongzip.dobong.global.security.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final AuthenticatedProvider authenticatedProvider;
    private final PasswordEncoder passwordEncoder;

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

        // 닉네임이 들어왔을 때만 변경
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        // 생일이 들어왔을 때만 변경
        if (dto.getBirth() != null) {
            user.setBirth(dto.getBirth());
        }
        // 이메일이 바뀔 때만 중복 검사 후 변경
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            boolean exists = userRepository.existsByEmailAndLoginType(dto.getEmail(), user.getLoginType());
            if (exists) {
                throw new BusinessException(StatusCode.USER_ALREADY_EXISTS);
            }
            user.setEmail(dto.getEmail());
        }
    }


    @Transactional
    public void changePassword(PasswordChangeRequestDto dto) {
        User user = authenticatedProvider.getCurrentUser();

        //  소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.getLoginType() != LoginType.APP) {
            throw new BusinessException(StatusCode.NOT_ALLOWED_FOR_SOCIAL_LOGIN);
        }

        // 1. 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_CURRENT_PASSWORD);
        }

        // 2. 새 비밀번호 형식 검증
        PasswordValidator.validate(dto.getNewPassword());

        // 3. 새 비밀번호 확인값 일치 확인
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(StatusCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        // 4. 새 비밀번호 저장
        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

}
