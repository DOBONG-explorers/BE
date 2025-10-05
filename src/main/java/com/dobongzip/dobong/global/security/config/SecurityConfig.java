package com.dobongzip.dobong.global.security.config;
import com.dobongzip.dobong.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (로그인 불필요)
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/mainpage/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/places/dobong",
                                "/api/v1/places/*",           // 상세
                                "/api/v1/places/*/reviews"    // 리뷰 목록
                        ).permitAll()

                        // 쓰기/수정/삭제는 로그인 필요
                        .requestMatchers(HttpMethod.POST,   "/api/v1/places/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/places/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/places/*/reviews/*").authenticated()

                        // 그 외는 기본적으로 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

