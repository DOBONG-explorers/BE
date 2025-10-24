package com.dobongzip.dobong.global.security.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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

        return new OpenAPI()
                .info(info)
                .servers(List.of(productionServer, localServer));
    }
}