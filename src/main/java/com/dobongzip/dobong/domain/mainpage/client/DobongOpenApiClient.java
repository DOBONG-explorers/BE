package com.dobongzip.dobong.domain.mainpage.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Component
public class DobongOpenApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String contCode;
    private final String type;

    public DobongOpenApiClient(
            @Value("${spring.dobong.api.base-url}") String baseUrl,
            @Value("${spring.dobong.api.api-key}") String apiKey,
            @Value("${spring.dobong.api.cont-code}") String contCode
    ) {
        this.restTemplate = new RestTemplate();
        // 한글 깨짐 방지 - UTF-8 설정
        this.restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
        );

        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.contCode = contCode;
        this.type = "json"; // 고정
    }

    public String requestDobongData() {
        String uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("api_key", apiKey)
                .queryParam("cont_code", contCode)
                .queryParam("type", type)
                .toUriString();

        return restTemplate.getForObject(uri, String.class);
    }
}
