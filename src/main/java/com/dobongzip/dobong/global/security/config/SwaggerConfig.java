package com.dobongzip.dobong.global.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("DobongZip API")
                .version("v1.0.0")
                .description("도봉집 프로젝트 API 명세서입니다.");

        Server productionServer = new Server()
                .url("https://dobongzip.com")
                .description("Production Server");

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");

        // 1. Bearer 토큰 인증 스키마 정의
        String securitySchemeName = "bearerAuth";
        SecurityScheme bearerAuthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // 인증 타입
                .scheme("bearer")               // 스키마: Bearer
                .bearerFormat("JWT")            // 포맷: JWT
                .in(SecurityScheme.In.HEADER)   // 위치: 헤더
                .name("Authorization");         // 헤더 이름

        // 2. 전역 보안 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(securitySchemeName);

        return new OpenAPI()
                .info(info)
                .servers(List.of(productionServer, localServer))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, bearerAuthScheme))
                .addSecurityItem(securityRequirement);
    }
}