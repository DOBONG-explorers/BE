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

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String); // "anonymousUser" 방지
    }

    /** 현재 SecurityContext의 Principal(CustomUserDetails) 또는 null */
    public CustomUserDetails getPrincipalOrNull() {
        if (!isAuthenticated()) return null;
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (p instanceof CustomUserDetails cud) ? cud : null;
    }

    /** 현재 Principal 없으면 401 던짐 */
    public CustomUserDetails getPrincipalOrThrow() {
        CustomUserDetails p = getPrincipalOrNull();
        if (p == null) throw BusinessException.of(StatusCode.REVIEW_UNAUTHORIZED);
        return p;
    }

    /** 현재 사용자 엔티티(있으면 반환, 없으면 404) */
    public User getCurrentUser() {
        CustomUserDetails userDetails = getPrincipalOrThrow();
        return userRepository.findByEmailAndLoginType(userDetails.getEmail(), userDetails.getLoginType())
                .orElseThrow(() -> BusinessException.of(StatusCode.USER_NOT_FOUND));
    }

    /** 현재 사용자 id or null */
    public Long currentUserIdOrNull() {
        CustomUserDetails p = getPrincipalOrNull();
        return (p != null) ? p.getId() : null;
    }
}
