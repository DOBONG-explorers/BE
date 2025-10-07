package com.dobongzip.dobong.domain.mainpage.service;

import com.dobongzip.dobong.domain.mainpage.client.DobongOpenApiClient;
import com.dobongzip.dobong.domain.mainpage.client.SeoulEventClient;
import com.dobongzip.dobong.domain.mainpage.dto.request.EventSearchRequest;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.SeoulEventResponse;
import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final GooglePlacesClientV1 placesClient;

    private final SeoulEventClient client;
    private final DobongOpenApiClient dobongOpenApiClient;

    /** 도봉 오늘의 행사 목록 */
    public List<EventDto> getDobongToday(EventSearchRequest req) {
        // 1) 날짜 파싱(+ 검증)
        String dateStr = (req.getDate() == null || req.getDate().isBlank())
                ? LocalDate.now(KST).format(F)
                : req.getDate();

        final LocalDate target;
        try {
            target = LocalDate.parse(dateStr, F);
        } catch (DateTimeParseException ex) {
            // 잘못된 요청 → 400
            throw BusinessException.of(StatusCode.INVALID_DATE_FORMAT);
        }

        // 2) 외부 API 호출
        final SeoulEventResponse res;
        try {
            res = client.callAll(); // 필요 시 callAllRowsPaged 로 교체 가능
        } catch (Exception ex) {
            log.error("[SeoulEvent] API call failed", ex);
            // 외부 API 호출 실패 → 502
            throw BusinessException.of(StatusCode.SEOUL_EVENT_API_FAILED);
        }

        // 3) 응답 검증
        if (res == null
                || res.getCulturalEventInfo() == null
                || res.getCulturalEventInfo().getRow() == null) {
            // 응답 포맷 이상 → 502
            throw BusinessException.of(StatusCode.SEOUL_EVENT_API_BAD_RESPONSE);
        }

        List<EventDto> rows = res.getCulturalEventInfo().getRow();

        // ── 진단 로그(유지) ──
        log.info("RAW_TOTAL={}", rows.size());
        rows.stream().map(EventDto::getGuName).filter(Objects::nonNull)
                .map(String::trim).distinct().sorted()
                .forEach(gu -> log.info("GUNAME_DIST={}", gu));
        rows.stream().limit(2).forEach(r -> {
            try { log.info("SAMPLE_ROW={}", new ObjectMapper().writeValueAsString(r)); }
            catch (Exception ignore) {}
        });

        // 4) 필터링
        List<EventDto> result = rows.stream()
                .filter(e -> isTodayIncluded(e, target))
                .filter(this::isDobong)
                .toList();

        // 카운트 로그
        long todayOnly = rows.stream().filter(e -> isTodayIncluded(e, target)).count();
        long dobongOnly = rows.stream().filter(this::isDobong).count();
        log.info("COUNTS total={}, todayOnly={}, dobongOnly={}, final={}",
                rows.size(), todayOnly, dobongOnly, result.size());

        // 비즈니스적으로 "없음"은 정상 케이스이므로 빈 리스트 반환(에러 아님)
        return result;
    }

    /** 도봉 문화유산(구청 오픈API) */
    public JsonNode getDobongCulturalHeritage() {
        final String response;
        try {
            response = dobongOpenApiClient.requestDobongData();
        } catch (Exception ex) {
            log.error("[DobongOpenAPI] API call failed", ex);
            throw BusinessException.of(StatusCode.DOBONG_OPENAPI_FAILED);
        }

        if (response == null || response.isBlank()) {
            throw BusinessException.of(StatusCode.DOBONG_OPENAPI_BAD_RESPONSE);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode outer = mapper.readTree(response);

            JsonNode dataNode = outer.get("CONT_DATA_ROW");
            if (dataNode == null || !dataNode.isArray()) {
                throw BusinessException.of(StatusCode.DOBONG_OPENAPI_BAD_RESPONSE);
            }

            // === Google Places 사진 주입 ===
            for (JsonNode item : dataNode) {
                String name = safeText(item, "SHD_NM");
                // 도로명 주소 우선, 없으면 지번
                String addr = firstNonBlank(
                        safeText(item, "CO_F2"),
                        safeText(item, "SCD_JIBUN_ADDR")
                );
                Double lat = safeDouble(item, "LATITUDE");
                Double lng = safeDouble(item, "LONGITUDE");

                String query = (name + " " + Optional.ofNullable(addr).orElse("")).trim();

                Optional<String> photoUrl = Optional.empty();
                if (!query.isBlank()) {
                    photoUrl = placesClient.searchFirstPhotoUrlByText(query, lat, lng, 800);
                }

                if (item.isObject()) {
                    ((ObjectNode) item).put(
                            "IMAGE_URL",
                            photoUrl.orElse("https://your.cdn/static/placeholder.png")
                    );
                }
            }

            return dataNode;

        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("[DobongOpenAPI] JSON parse failed", ex);
            throw BusinessException.of(StatusCode.DOBONG_OPENAPI_BAD_RESPONSE);
        }
    }

    // ───────────────────────── private helpers ─────────────────────────

    private boolean isDobong(EventDto e) {
        String gu = Optional.ofNullable(e.getGuName()).orElse("").trim();
        if ("도봉구".equals(gu)) return true;
        // 보조: 장소 텍스트로 판정
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
    // ====== 헬퍼 ======
    private String safeText(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? "" : v.asText("");
    }
    private String firstNonBlank(String... ss) {
        for (String s : ss) if (s != null && !s.isBlank()) return s;
        return "";
    }
    private Double safeDouble(JsonNode n, String field) {
        String s = safeText(n, field);
        if (s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (Exception ignored) { return null; }
    }
}
