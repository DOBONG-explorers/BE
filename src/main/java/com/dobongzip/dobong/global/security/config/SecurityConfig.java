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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                        // 공개
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/mainpage/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/places/dobong",
                                "/api/v1/places/*",              // 상세
                                "/api/v1/places/*/reviews"       // 리뷰 목록
                        ).permitAll()

                        //  좋아요 관련은 서비스에서 로그인 강제
                        .requestMatchers(HttpMethod.POST,   "/api/v1/places/*/like").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/places/*/like").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/api/v1/places/likes/me").permitAll()

                        // 리뷰 쓰기/수정/삭제는 필터 단계에서 로그인 강제
                        .requestMatchers(HttpMethod.POST,   "/api/v1/places/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/places/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/places/*/reviews/*").authenticated()

                        // 프리플라이트 허용 (브라우저 호출이라면 필수)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:3000", "https://your-domain.com")); // 필요 origin
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

}

