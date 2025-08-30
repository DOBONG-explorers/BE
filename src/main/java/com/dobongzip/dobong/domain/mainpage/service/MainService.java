package com.dobongzip.dobong.domain.mainpage.service;

import com.dobongzip.dobong.domain.mainpage.client.DobongOpenApiClient;
import com.dobongzip.dobong.domain.mainpage.client.SeoulEventClient;
import com.dobongzip.dobong.domain.mainpage.dto.request.EventSearchRequest;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.SeoulEventResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SeoulEventClient client;

    private final DobongOpenApiClient dobongOpenApiClient;

    public List<EventDto> getDobongToday(EventSearchRequest req) {
        String dateStr = (req.getDate()==null || req.getDate().isBlank())
                ? LocalDate.now(KST).format(F) : req.getDate();
        LocalDate target = LocalDate.parse(dateStr, F);

        // 1) 전수 호출(권장) — 필요 시 client.callAllRowsPaged(500)로 변경
        SeoulEventResponse res = client.callAll();

        List<EventDto> rows = Optional.ofNullable(res)
                .map(SeoulEventResponse::getCulturalEventInfo)
                .map(SeoulEventResponse.Body::getRow)
                .orElse(List.of());

        // ── 진단 로그 ──
        log.info("RAW_TOTAL={}", rows.size());
        rows.stream().map(EventDto::getGuName).filter(Objects::nonNull)
                .map(String::trim).distinct().sorted()
                .forEach(gu -> log.info("GUNAME_DIST={}", gu));
        rows.stream().limit(2).forEach(r -> {
            try { log.info("SAMPLE_ROW={}", new ObjectMapper().writeValueAsString(r)); }
            catch (Exception ignore) {}
        });

        // 2) 날짜 포함 + 도봉구 판정(PLACE 보조) 필터
        List<EventDto> result = rows.stream()
                .filter(e -> isTodayIncluded(e, target))
                .filter(this::isDobong)
                .toList();

        // 카운트 확인
        long todayOnly = rows.stream().filter(e -> isTodayIncluded(e, target)).count();
        long dobongOnly = rows.stream().filter(this::isDobong).count();
        log.info("COUNTS total={}, todayOnly={}, dobongOnly={}, final={}",
                rows.size(), todayOnly, dobongOnly, result.size());

        return result;
    }

    private boolean isDobong(EventDto e) {
        String gu = Optional.ofNullable(e.getGuName()).orElse("").trim();
        if ("도봉구".equals(gu)) return true;
        // 보조: 장소에만 표기된 경우
        String place = Optional.ofNullable(e.getPlace()).orElse("");
        return place.contains("도봉구") || place.contains("도봉");
    }

    private boolean isTodayIncluded(EventDto e, LocalDate today) {
        LocalDate d = parseDateLoose(e.getDate());
        if (d != null && d.equals(today)) return true;

        LocalDate s = parseDateLoose(e.getStartDate());
        LocalDate t = parseDateLoose(e.getEndDate());
        if (s == null && t == null) return false;
        if (s == null) s = today;
        if (t == null) t = today;
        return !today.isBefore(s) && !today.isAfter(t);
    }

    private LocalDate parseDateLoose(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        // "2025-08-11 00:00:00" or "2025-08-11T00:00:00"
        if (s.length() > 10 && (s.charAt(10) == ' ' || s.charAt(10) == 'T')) {
            s = s.substring(0, 10);
        }
        // 8자리 숫자 → yyyyMMdd
        if (s.matches("\\d{8}")) {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        // 점(.) → 하이픈(-)
        s = s.replace('.', '-');
        try { return LocalDate.parse(s, F); }
        catch (Exception ex) { return null; }
    }

    public JsonNode getDobongCulturalHeritage() {
        try {
            String response = dobongOpenApiClient.requestDobongData();
            ObjectMapper mapper = new ObjectMapper();

            JsonNode outer = mapper.readTree(response);

            // 기존 "data" 대신 실제 key 사용
            JsonNode dataNode = outer.get("CONT_DATA_ROW");
            if (dataNode == null) {
                throw new IllegalStateException("'CONT_DATA_ROW' 필드가 없습니다. 응답: " + outer.toPrettyString());
            }
            return dataNode;
        } catch (Exception e) {
            throw new RuntimeException("도봉 문화유산 데이터 처리 중 오류", e);
        }
    }


}