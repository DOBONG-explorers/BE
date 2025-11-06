package com.dobongzip.dobong.domain.mainpage.service;

import com.dobongzip.dobong.domain.mainpage.client.DobongOpenApiClient;
import com.dobongzip.dobong.domain.mainpage.client.SeoulEventClient;
import com.dobongzip.dobong.domain.mainpage.dto.request.EventSearchRequest;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventImageDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.EventListItemDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.HeritageDetailDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.HeritageListItemDto;
import com.dobongzip.dobong.domain.mainpage.dto.response.SeoulEventResponse;
import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.domain.map.dto.response.TopPlaceDto;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

            // === Google Places 사진 + 안정적 ID 주입 ===
            for (JsonNode item : dataNode) {
                String name = safeText(item, "SHD_NM");
                String addr = firstNonBlank(safeText(item, "CO_F2"), safeText(item, "SCD_JIBUN_ADDR"));
                Double lat = safeDouble(item, "LATITUDE");
                Double lng = safeDouble(item, "LONGITUDE");
                String query = (name + " " + Optional.ofNullable(addr).orElse("")).trim();

                String img = "https://your.cdn/static/placeholder.png";
                if (!query.isBlank()) {
                    try {
                        Optional<String> photoUrl = placesClient.searchFirstPhotoUrlByText(query, lat, lng, 800);
                        if (photoUrl.isPresent() && !photoUrl.get().isBlank()) img = photoUrl.get();
                    } catch (Exception photoEx) {
                        log.warn("[DobongOpenAPI] Places photo fetch failed for '{}'", query, photoEx);
                    }
                }

                if (item.isObject()) {
                    ObjectNode o = (ObjectNode) item;
                    o.put("IMAGE_URL", img);
                    // 이름+주소(+번호 보조)로 안정적 ID 주입 → 상세조회에서 그대로 사용할 것
                    String computedId = stableId(
                            name,
                            addr,
                            safeText(item, "NUMBER") // 없으면 빈문자
                    );
                    o.put("ID", computedId);
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
    /** 문화유산 상세 (원본 JSON 그대로 반환) */
    public JsonNode getDobongHeritageDetailRaw(String id) {
        if (id == null || id.isBlank()) {
            throw BusinessException.of(StatusCode.INVALID_REQUEST);
        }

        JsonNode data = getDobongCulturalHeritage(); // IMAGE_URL, ID 포함된 전체 JSON
        for (JsonNode item : data) {
            if (id.equals(safeText(item, "ID"))) {
                return item; // 그대로 반환 (JsonNode)
            }
        }

        throw BusinessException.of(StatusCode.RESOURCE_NOT_FOUND);
    }

    // ───────────────────── 신규: 목록/상세(행사) 공개 메서드 ─────────────────────

    /** 도봉 행사 이미지들만 */
    public List<EventImageDto> getDobongEventImages(EventSearchRequest req) {
        List<EventDto> events = getDobongToday(req); // 이미 도봉 + 날짜 필터링됨
        return events.stream()
                .filter(e -> e.getMainImg() != null && !e.getMainImg().isBlank())
                .map(e -> EventImageDto.builder()
                        .id(stableEventId(e))      // ★ id 추가
                        .imageUrl(e.getMainImg())  // ★ 이미지 URL
                        .build())
                .toList();
    }


    /** 도봉 행사 리스트(이름 + 날짜) */
    public List<EventListItemDto> listDobongEvents(EventSearchRequest req) {
        List<EventDto> events = getDobongToday(req);
        return events.stream()
                .map(e -> EventListItemDto.builder()
                        .id(stableEventId(e))
                        .title(e.getTitle())
                        .dateText(buildDateText(e.getDate(), e.getStartDate(), e.getEndDate()))
                        .build())
                .toList();
    }

    /** 도봉 행사 상세: EventDto 그대로 반환 */
    public EventDto getDobongEventDetailRaw(String id, EventSearchRequest req) {
        if (id == null || id.isBlank()) {
            throw BusinessException.of(StatusCode.INVALID_REQUEST);
        }
        return getDobongToday(req).stream()
                .filter(e -> stableEventId(e).equals(id))
                .findFirst()
                .orElseThrow(() -> BusinessException.of(StatusCode.RESOURCE_NOT_FOUND));
    }



    // ───────────────────── 신규: 목록/상세(문화유산) 공개 메서드 ─────────────────────

    /** 문화유산 목록(사진+이름) */
    public List<HeritageListItemDto> listDobongHeritage() {
        JsonNode data = getDobongCulturalHeritage(); // IMAGE_URL, ID 주입 완료
        List<HeritageListItemDto> list = new ArrayList<>();
        for (JsonNode item : data) {
            list.add(HeritageListItemDto.builder()
                    .id(safeText(item, "ID"))                 // 재계산 대신 주입된 ID 사용
                    .name(safeText(item, "SHD_NM"))
                    .imageUrl(safeText(item, "IMAGE_URL"))
                    .build());
        }
        return list;
    }

    /** 문화유산 상세: HeritageDetailDto로 매핑 반환 */
    public HeritageDetailDto getDobongHeritageDetail(String id) {
        if (id == null || id.isBlank()) {
            throw BusinessException.of(StatusCode.INVALID_REQUEST);
        }

        JsonNode data = getDobongCulturalHeritage(); // 전체 목록
        for (JsonNode item : data) {
            if (id.equals(safeText(item, "ID"))) {
                return HeritageDetailDto.builder()
                        .id(safeText(item, "ID"))
                        .name(safeText(item, "SHD_NM"))
                        .nameHanja(safeText(item, "SHD_NM_HANJA"))
                        .address(firstNonBlank(
                                safeText(item, "CO_F2"),
                                safeText(item, "SCD_JIBUN_ADDR")))
                        .designationNo(safeText(item, "NUMBER"))
                        .designationDate(normalizeDate(safeText(item, "REG_DT")))
                        .tel(safeText(item, "CO_F3"))
                        .description(safeText(item, "CONT_CO_F1"))
                        .imageUrl(firstNonBlank(
                                safeText(item, "IMAGE_URL"),
                                "https://your.cdn/static/placeholder.png"))
                        .lat(safeDouble(item, "LATITUDE"))
                        .lng(safeDouble(item, "LONGITUDE"))
                        .build();
            }
        }

        throw BusinessException.of(StatusCode.RESOURCE_NOT_FOUND);
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

    /** 날짜 문자열을 yyyy-MM-dd로 정규화(모르면 원문 유지) */
    private String normalizeDate(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.matches("\\d{8}")) { // yyyyMMdd
            return LocalDate.parse(t, DateTimeFormatter.ofPattern("yyyyMMdd")).toString();
        }
        t = t.replace('.', '-');
        if (t.length() > 10 && (t.charAt(10) == ' ' || t.charAt(10) == 'T')) {
            t = t.substring(0, 10);
        }
        try { return LocalDate.parse(t, F).toString(); }
        catch (Exception ignore) { return s; } // 모르면 원문 유지
    }

    private String buildDateText(String date, String start, String end) {
        String d = (date == null) ? "" : date.trim();
        String s = (start == null) ? "" : start.trim();
        String t = (end == null) ? "" : end.trim();

        if (!d.isBlank()) return d;                // 단일 날짜 우선
        if (!s.isBlank() && !t.isBlank()) return s + " ~ " + t;
        if (!s.isBlank()) return s;
        if (!t.isBlank()) return t;
        return ""; // 없을 수도 있음
    }

    /** 행사 목록에서 안정적 상세 ID 생성 */
    private String stableEventId(EventDto e) {
        String key = String.join("|",
                nullToEmpty(e.getTitle()),
                nullToEmpty(e.getDate()),
                nullToEmpty(e.getStartDate()),
                nullToEmpty(e.getEndDate()),
                nullToEmpty(e.getPlace())
        ).toLowerCase().trim();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) sb.append(String.format("%02x", h[i])); // 18 hex
            return sb.toString();
        } catch (Exception ex) {
            return String.valueOf(key.hashCode());
        }
    }

    private String nullToEmpty(String s){ return (s==null)? "" : s; }

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

    /** 문화유산용 안정적 ID 생성(이름+주소+번호) */
    private String stableId(String name, String address, String numberOrEmpty) {
        String key = (Optional.ofNullable(name).orElse("").trim() + "|" +
                Optional.ofNullable(address).orElse("").trim() + "|" +
                Optional.ofNullable(numberOrEmpty).orElse("").trim()
        ).toLowerCase();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) sb.append(String.format("%02x", h[i])); // 18 hex
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(key.hashCode());
        }
    }

    @Value("classpath:hotplace/nh.json")
    private ClassPathResource resource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 랜덤 장소 반환하는 로직
    public TopPlaceDto getRandomPlaceFromJson() {
        List<TopPlaceDto> places = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());

            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    TopPlaceDto place = new TopPlaceDto();
                    place.setPlaceId(node.path("placeId").asText());
                    place.setName(node.path("name").asText());
                    place.setAddress(node.path("address").asText());
                    place.setImageUrl(node.path("imageUrl").asText());

                    places.add(place);
                }
            }

            // 랜덤 선택
            Random random = new Random();
            int index = random.nextInt(places.size());
            return places.get(index);  // 랜덤으로 하나 반환

        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON file", e);
        }
    }
}
