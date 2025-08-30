package com.dobongzip.dobong.global.security.jwt;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedProvider {

    private final UserRepository userRepository;

    /**
     * ✅ 현재 로그인한 사용자의 User 엔티티를 반환
     */
    public User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByEmailAndLoginType(
                userDetails.getEmail(),
                userDetails.getLoginType()
        ).orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));
    }


    /**
     * ✅ 현재 인증된 사용자 여부 확인
     */
    public boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String); // "anonymousUser" 방지
    }
}
