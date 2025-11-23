package com.dobongzip.dobong.domain.mainpage.client;

import com.dobongzip.dobong.domain.mainpage.dto.response.SeoulEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventDto;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SeoulEventClient {

    @Value("${spring.seoul.openapi.key}")
    private String apiKey;
    @Value("${spring.seoul.api.base-url}")
    private String baseUrl;

    private final WebClient webClient;

    public SeoulEventClient() {
        var strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024)) // 4MB
                .build();
        this.webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }

    /** 날짜 없이 전수 호출 (1~1000) */
    public SeoulEventResponse callAll() {
        String url = String.format("%s/%s/json/culturalEventInfo/1/1000", baseUrl, apiKey);
        log.info("[SeoulAPI] URL={}", url);
        return webClient.get().uri(url).retrieve().bodyToMono(SeoulEventResponse.class).block();
    }

    /** (옵션) 페이지 분할 호출: 메모리 더 아끼고 싶을 때 사용 */
    public List<EventDto> callAllRowsPaged(int pageSize) {
        List<EventDto> all = new ArrayList<>();
        int start = 1;
        while (true) {
            int end = start + pageSize - 1;
            String url = String.format("%s/%s/json/culturalEventInfo/%d/%d", baseUrl, apiKey, start, end);
            log.info("[SeoulAPI] URL={}", url);
            SeoulEventResponse res = webClient.get().uri(url).retrieve()
                    .bodyToMono(SeoulEventResponse.class).block();

            List<EventDto> rows = Optional.ofNullable(res)
                    .map(SeoulEventResponse::getCulturalEventInfo)
                    .map(SeoulEventResponse.Body::getRow)
                    .orElse(List.of());
            all.addAll(rows);

            if (rows.size() < pageSize) break; // 마지막 페이지
            start += pageSize;
            if (start > 10000) break; // 안전장치
        }
        return all;
    }
}

