package com.dobongzip.dobong.global.security.jwt;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedProvider {

    private final UserRepository userRepository;

    /** 로그인 유저(필수) — 없으면 401 */
    public User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByEmailAndLoginType(
                userDetails.getEmail(),
                userDetails.getLoginType()
        ).orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));
    }

    /** 인증 여부(anonymous 제외) */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String);
    }

    /** 로그인 유저 id — 비로그인/anonymous면 null */
    public Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            return null;
        }
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return principal.getId();
    }
}
