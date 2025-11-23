package com.dobongzip.dobong.global.security.jwt;

import com.dobongzip.dobong.domain.user.repository.UserRepository;
import com.dobongzip.dobong.global.security.details.CustomUserDetails;
import com.dobongzip.dobong.global.security.enums.LoginType;
import com.dobongzip.dobong.global.security.util.JwtUtil;
import com.dobongzip.dobong.global.security.service.JwtBlacklistService; // ★ 추가
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtBlacklistService jwtBlacklistService; // ★ 추가

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7).trim() : null;

        if (token == null) {
            // 주소창 직접 호출 등
            chain.doFilter(req, res);
            return;
        }

        // (1) 블랙리스트?
        boolean blocked = jwtBlacklistService.isBlocked(token);
        if (blocked) {
            System.out.println("[AUTH] 401 reason=blacklist uri=" + req.getRequestURI());
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // (2) 토큰 유효성?
        boolean valid;
        try { valid = jwtUtil.validateToken(token); }
        catch (Exception e) {
            System.out.println("[AUTH] 401 reason=validateException msg=" + e.getMessage());
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!valid) {
            System.out.println("[AUTH] 401 reason=invalidToken");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // (3) 사용자 상태?
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtUtil.extractEmail(token);
            var type = LoginType.valueOf(jwtUtil.extractLoginType(token));
            var user = userRepository.findByEmailAndLoginType(email, type)
                    .orElse(null);

            if (user == null) {
                System.out.println("[AUTH] 401 reason=userNotFound email=" + email + " type=" + type);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            System.out.println("[AUTH] user active=" + user.isActive() + " deletedAt=" + user.getDeletedAt());

            if (!user.isActive() || user.isDeleted()) {
                System.out.println("[AUTH] 401 reason=inactiveOrDeleted email=" + email);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            var details = new CustomUserDetails(user);
            var authTok = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authTok);
        }

        chain.doFilter(req, res);
    }


}
