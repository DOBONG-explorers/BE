package com.dobongzip.dobong.domain.map.client;

import com.dobongzip.dobong.domain.map.dto.response.PlacesV1PlaceDetailsResponse;
import com.dobongzip.dobong.domain.map.dto.response.PlacesV1SearchTextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClientV1 {

    private static final String BASE = "https://places.googleapis.com/v1";

    private final RestTemplate restTemplate;
    private final GooglePlacesProperties props;

    /** 검색용 필드마스크 (카드 목록) */
    private static final String SEARCH_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.googleMapsUri",
            "places.location",
            "places.priceLevel",
            "places.photos",
            "places.currentOpeningHours.weekdayDescriptions",
            "places.rating",
            "places.userRatingCount",
            // 소개 관련
            "places.editorialSummary",
            "places.generativeSummary.overview",
            "places.generativeSummary.description",
            // 보조 맥락
            "places.areaSummary",
            "places.addressDescriptor"
    );

    /** 상세용 필드마스크 (상세 화면) */
    private static final String DETAIL_FIELD_MASK = String.join(",",
            "id",
            "displayName",
            "formattedAddress",
            "internationalPhoneNumber",
            "nationalPhoneNumber",
            "currentOpeningHours.weekdayDescriptions",
            "priceLevel",
            "rating",
            "userRatingCount",
            "photos",
            // 소개(최우선)
            "editorialSummary",
            "generativeSummary.overview",
            "generativeSummary.description",
            // 보조 맥락
            "areaSummary",
            "addressDescriptor",
            // 위키 geosearch용 좌표
            "location"
    );

    /** 리뷰용 필드마스크(최소) */
    private static final String REVIEWS_FIELD_MASK = String.join(",",
            "id",
            "rating",
            "userRatingCount",
            "reviews"
    );

    // ==========================
    // 검색(목록)
    // ==========================
    public PlacesV1SearchTextResponse searchDobongAttractions() {
        URI uri = URI.create(BASE + "/places:searchText");
        HttpHeaders headers = newHeaders(SEARCH_FIELD_MASK);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "textQuery", "도봉구 명소",
                "languageCode", props.getLanguage(),
                "regionCode",  props.getRegion()
        );

        ResponseEntity<PlacesV1SearchTextResponse> res =
                postJson(uri, headers, body, PlacesV1SearchTextResponse.class);

        log.info("[PLACES v1 search] http={} size={}",
                res.getStatusCodeValue(),
                res.getBody() != null && res.getBody().getPlaces() != null
                        ? res.getBody().getPlaces().size() : 0);

        return res.getBody();
    }

    // ==========================
    // 상세
    // ==========================
    public PlacesV1PlaceDetailsResponse fetchPlaceDetails(String placeId) {
        String id = normalizePlaceId(placeId);
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE + "/places/" + id)
                .queryParam("languageCode", props.getLanguage())
                .queryParam("regionCode", props.getRegion())
                .build(true)
                .toUri();

        HttpHeaders headers = newHeaders(DETAIL_FIELD_MASK);

        ResponseEntity<PlacesV1PlaceDetailsResponse> res =
                exchangeGet(uri, headers, PlacesV1PlaceDetailsResponse.class);

        return res.getBody();
    }

    // ==========================
    // 리뷰 전용
    // ==========================
    public PlacesV1PlaceDetailsResponse fetchPlaceReviews(String placeId) {
        String id = normalizePlaceId(placeId);
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE + "/places/" + id)
                .queryParam("languageCode", props.getLanguage())
                .queryParam("regionCode", props.getRegion())
                .build(true)
                .toUri();

        HttpHeaders headers = newHeaders(REVIEWS_FIELD_MASK);

        ResponseEntity<PlacesV1PlaceDetailsResponse> res =
                exchangeGet(uri, headers, PlacesV1PlaceDetailsResponse.class);

        return res.getBody();
    }

    // ==========================
    // 유틸
    // ==========================
    private HttpHeaders newHeaders(String fieldMask) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Goog-Api-Key", props.getApiKey());
        headers.set("X-Goog-FieldMask", fieldMask);
        headers.set("Accept-Language", props.getLanguage());
        return headers;
    }

    private <T> ResponseEntity<T> exchangeGet(URI uri, HttpHeaders headers, Class<T> type) {
        try {
            return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), type);
        } catch (HttpStatusCodeException e) {
            log.error("[PLACES v1 GET] uri={} status={} body={}",
                    uri, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private <T> ResponseEntity<T> postJson(URI uri, HttpHeaders headers, Object body, Class<T> type) {
        try {
            return restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(body, headers), type);
        } catch (HttpStatusCodeException e) {
            log.error("[PLACES v1 POST] {} -> status={} body={}",
                    uri, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public String buildPhotoUrl(String photoName, int maxWidthPx) {
        if (photoName == null) return null;
        int w = Math.max(100, Math.min(maxWidthPx, 1600));
        return BASE + "/" + photoName + "/media?maxWidthPx=" + w + "&key=" + props.getApiKey();
    }

    public String buildMapsUrl(String googleMapsUri) {
        return googleMapsUri;
    }

    private String normalizePlaceId(String placeId) {
        if (placeId == null) return "";
        return placeId.startsWith("places/") ? placeId.substring("places/".length()) : placeId;
    }

    public Optional<String> searchFirstPhotoUrlByText(String textQuery, Double lat, Double lng, int maxWidthPx) {
        URI uri = URI.create(BASE + "/places:searchText");
        HttpHeaders headers = newHeaders(SEARCH_FIELD_MASK);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", props.getLanguage());
        body.put("regionCode", props.getRegion());

        // 좌표가 있으면 Location Bias (반경 300m)
        if (lat != null && lng != null) {
            body.put("locationBias", Map.of(
                    "circle", Map.of(
                            "center", Map.of(
                                    "latitude", lat,
                                    "longitude", lng
                            ),
                            "radius", 300.0
                    )
            ));
        }

        try {
            // places:searchText 응답을 JsonNode로 받아 처리 (DTO 없어도 OK)
            ResponseEntity<JsonNode> res = postJson(uri, headers, body, JsonNode.class);
            JsonNode root = res.getBody();
            if (root == null || !root.has("places")) return Optional.empty();

            JsonNode places = root.get("places");
            if (!places.isArray() || places.isEmpty()) return Optional.empty();

            // 1순위 후보 선택 (필요시 더 정교한 스코어링 가능)
            JsonNode first = places.get(0);
            JsonNode photos = first.path("photos");
            if (!photos.isArray() || photos.isEmpty()) return Optional.empty();

            // v1은 photo.name 형태 ("places/xxx/photos/yyy")
            String photoName = photos.get(0).path("name").asText(null);
            if (photoName == null || photoName.isBlank()) return Optional.empty();

            return Optional.ofNullable(buildPhotoUrl(photoName, maxWidthPx));
        } catch (HttpStatusCodeException e) {
            log.warn("[PLACES v1 searchFirstPhotoUrlByText] status={} body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[PLACES v1 searchFirstPhotoUrlByText] fail query={}", textQuery, e);
            return Optional.empty();
        }
}
}
