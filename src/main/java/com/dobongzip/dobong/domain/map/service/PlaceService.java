package com.dobongzip.dobong.domain.map.service;

import com.dobongzip.dobong.domain.like.service.LikeService;
import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.domain.map.client.WikipediaClient;
import com.dobongzip.dobong.domain.map.dto.response.*;
import com.dobongzip.dobong.domain.map.entity.PlaceStat;
import com.dobongzip.dobong.domain.map.repository.PlaceStatRepository;
import com.dobongzip.dobong.domain.map.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PlaceService {
    private final GooglePlacesClientV1 v1;
    private final WikipediaClient wikipedia;
    private final LikeService likeService;
    private final PlaceStatRepository placeStatRepository;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");


    @Transactional(readOnly = true)
    public List<PredictionProjection> getAutocompleteFromDobongApi(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        //  1. V1 클라이언트의 동적 검색 메서드를 호출합니다.
        PlacesV1SearchTextResponse res = v1.searchPlacesByQuery(query);

        if (res == null || res.getPlaces() == null || res.getPlaces().isEmpty()) {
            return Collections.emptyList();
        }

        // 2. PlaceDto 리스트 생성 (findDobongAttractions의 1차 로직 재현)
        //  findDobongAttractions는 그대로 두고, 이 부분에서 PlaceDto를 직접 생성해야 합니다.
        List<PlaceDto> dobongPlaces = new ArrayList<>();
        for (var p : res.getPlaces()) {
            if (p.getLocation() == null) continue;

            // *주의*: findDobongAttractions의 상세 조회/정렬 로직은 생략
            dobongPlaces.add(PlaceDto.builder()
                    .placeId(p.getId())
                    .name(p.getDisplayName() != null ? p.getDisplayName().getText() : null)
                    .address(p.getFormattedAddress())
                    .build());
        }

        // 3. 필터링 및 Projection (이전 답변의 로직 재사용)
        final String lowerQuery = query.toLowerCase();

        List<PredictionProjection> filteredResults = dobongPlaces.stream()
                .filter(p -> {
                    String name = p.getName() != null ? p.getName().toLowerCase() : "";
                    String address = p.getAddress() != null ? p.getAddress().toLowerCase() : "";
                    return name.contains(lowerQuery) || address.contains(lowerQuery);
                })
                .map(p -> new PredictionProjection() {
                    @Override public String getPlaceId() { return p.getPlaceId(); }
                    @Override public String getName() { return p.getName(); }
                })
                .collect(Collectors.toList());

        return filteredResults;
    }
    public List<PlaceDto> findDobongAttractions(double userLat, double userLng, int limit) {
        PlacesV1SearchTextResponse res = v1.searchDobongAttractions();
        if (res == null || res.getPlaces() == null || res.getPlaces().isEmpty()) {
            return Collections.emptyList();
        }

        // 1차: Text Search 결과로 기본 정보 구성
        var top = res.getPlaces().stream()
                .limit(Math.max(1, Math.min(limit * 2L, 30)))
                .toList();

        List<PlaceDto> rough = new ArrayList<>(top.size());
        for (var p : top) {
            if (p.getLocation() == null) continue;
            double lat = p.getLocation().getLatitude();
            double lng = p.getLocation().getLongitude();
            long dist = GeoUtils.haversineMeters(userLat, userLng, lat, lng);

            String photoName = (p.getPhotos() != null && !p.getPhotos().isEmpty())
                    ? p.getPhotos().get(0).getName() : null;

            // 카드용 소개(구글 데이터만 사용)
            String summary = pickSummaryFromSearch(p);

            rough.add(PlaceDto.builder()
                    .placeId(p.getId())
                    .name(p.getDisplayName() != null ? p.getDisplayName().getText() : null)
                    .address(p.getFormattedAddress())
                    .latitude(lat)
                    .longitude(lng)
                    .distanceMeters(dist)
                    .distanceText(GeoUtils.formatDistance(dist))
                    .imageUrl(v1.buildPhotoUrl(photoName, 800))
                    .description(summary)
                    .openingHours(p.getCurrentOpeningHours() != null ? p.getCurrentOpeningHours().getWeekdayDescriptions() : null)
                    .priceLevel(p.getPriceLevel())
                    .mapsUrl(v1.buildMapsUrl(p.getGoogleMapsUri()))
                    .rating(p.getRating())
                    .reviewCount(p.getUserRatingCount())
                    .build());
        }

        // 2차: 정렬 → 최종 상위 limit만 상세조회
        var primaries = rough.stream()
                .sorted(Comparator.comparingLong(PlaceDto::getDistanceMeters))
                .limit(Math.max(1, limit))
                .toList();

        List<PlaceDto> enriched = new ArrayList<>(primaries.size());
        for (PlaceDto base : primaries) {
            var details = v1.fetchPlaceDetails(base.getPlaceId());

            String phone = null;
            String desc  = base.getDescription(); // 카드에서 이미 있으면 유지(목록은 위키 미사용)

            if (details != null) {
                // phone: 국제 → 국내 우선
                phone = firstNonNull(details.getInternationalPhoneNumber(), details.getNationalPhoneNumber());

                // 상세 설명은 화면에서 위키를 쓰므로, 목록 단계에서는 구글 폴백만 유지
                if (desc == null) {
                    desc =  pickSummaryFromDetailsStrict(details);
                }
            }
            if (desc == null) desc = "자세한 설명이 없습니다.";

            enriched.add(base.toBuilder().phone(phone).description(desc).build());
        }

        return enriched;
    }

    public PlaceDetailsResponse getPlaceDetail(String placeId) {
        bumpView(placeId);
        var d = v1.fetchPlaceDetails(placeId);
        if (d == null) return null;

        String name = d.getDisplayName() != null ? d.getDisplayName().getText() : null;
        Double lat = (d.getLocation() != null) ? d.getLocation().getLatitude() : null;
        Double lng = (d.getLocation() != null) ? d.getLocation().getLongitude() : null;

        // 1) Wikipedia 우선
        String desc = wikipedia.getSummary(name, lat, lng).orElse(null);

        // 2) 위키 실패 → Google 소개 중 "문단형"만 사용 (addressDescriptor는 제외!)
        if (!nonEmpty(desc)) {
            desc = pickSummaryFromDetailsStrict(d); //  addressDescriptor 안 씀
        }

        if (!nonEmpty(desc)) desc = "자세한 설명이 없습니다.";

        String phone = firstNonNull(d.getInternationalPhoneNumber(), d.getNationalPhoneNumber());

        List<String> photoUrls = Optional.ofNullable(d.getPhotos())
                .orElseGet(List::of).stream()
                .limit(8)
                .map(p -> v1.buildPhotoUrl(p.getName(), 1280))
                .toList();
        boolean liked = likeService.isLikedForCurrentUser(placeId);

        return PlaceDetailsResponse.builder()
                .placeId(d.getId())
                .name(name)
                .address(d.getFormattedAddress())
                .description(desc)
                .openingHours(d.getCurrentOpeningHours() != null ? d.getCurrentOpeningHours().getWeekdayDescriptions() : null)
                .priceLevel(d.getPriceLevel())
                .phone(phone)
                .rating(d.getRating())
                .reviewCount(d.getUserRatingCount())
                .photos(photoUrls)
                .liked(liked)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopPlaceDto> getTopPlaces(double userLat, double userLng, int limit) {
        int n = Math.max(1, Math.min(limit, 10)); // 최대 10개, 최소 1개

        // 1. DB에서 인기 장소 통계 조회
        var stats = placeStatRepository.findTop(PageRequest.of(0, n)).getContent();

        List<TopPlaceDto> out = new ArrayList<>();

        // 2. DB에서 가져온 각 장소를 순회하며 Google API로 상세 정보 조회
        for (PlaceStat s : stats) {
            String placeId = s.getPlaceId(); // (로그용)

            try {
                // [핵심] try 블록 시작 (API 호출 전체를 감싼다)

                // 3. (실패 지점 1) Google API로 장소 상세 정보 호출
                var d = v1.fetchPlaceDetails(placeId);

                // API 호출이 예외를 던지지 않았더라도, 응답이 비어있을 수 있음
                if (d == null || d.getLocation() == null) {
                    log.warn("[getTopPlaces] Google API 응답이 null이거나 위치 정보가 없습니다. (placeId: {})", placeId);
                    continue; // 이 항목을 건너뛰고 다음 루프로 이동
                }

                // --- (여기부터는 d가 정상일 때만 실행됨) ---
                double lat = d.getLocation().getLatitude();
                double lng = d.getLocation().getLongitude();

                // 4. 거리 계산
                long distInMeters = GeoUtils.haversineMeters(userLat, userLng, lat, lng);
                double distInKm = distInMeters / 1000.0;
                distInKm = Math.round(distInKm * 100.0) / 100.0;

                // 5. 사진 URL, 전화번호 추출
                String photoName = (d.getPhotos() != null && !d.getPhotos().isEmpty())
                        ? d.getPhotos().get(0).getName() : null;
                String phone = firstNonNull(d.getInternationalPhoneNumber(), d.getNationalPhoneNumber());

                // 6. (실패 지점 2) Google API로 사진 URL 빌드
                String imageUrl = v1.buildPhotoUrl(photoName, 800);

                // 7. 최종 DTO에 추가
                out.add(TopPlaceDto.builder()
                        .placeId(d.getId())
                        .name(d.getDisplayName() != null ? d.getDisplayName().getText() : null)
                        .address(d.getFormattedAddress())
                        .latitude(lat)
                        .longitude(lng)
                        .distance(distInKm)
                        .distanceText(GeoUtils.formatDistance(distInMeters))
                        .imageUrl(imageUrl)
                        .phone(phone)
                        .rating(d.getRating())
                        .reviewCount(d.getUserRatingCount())
                        .build());

            } catch (Exception e) {
                log.error("[getTopPlaces] 'Top 장소' 처리 중 예외 발생 (placeId: {}) | 에러: {}",
                        placeId, e.getMessage());
            }
        }

        // 8. 성공한 항목들만 리스트로 반환
        return out;
    }



    // --------------------------
    // 내부: 조회수 +1
    // --------------------------
    @Transactional
    protected void bumpView(String placeId) {
        var now = LocalDateTime.now(KST);
        var stat = placeStatRepository.findById(placeId)
                .orElse(PlaceStat.builder()
                        .placeId(placeId)
                        .viewCount(0)
                        .lastViewedAt(now)
                        .build());
        stat.setViewCount(stat.getViewCount() + 1);
        stat.setLastViewedAt(now);
        placeStatRepository.save(stat);
    }

    // ==========================
    // 내부 헬퍼
    // ==========================
    private static String pickSummaryFromSearch(PlacesV1SearchTextResponse.Place p) {
        if (p.getEditorialSummary() != null && nonEmpty(p.getEditorialSummary().getOverview())) {
            return p.getEditorialSummary().getOverview();
        }
        if (p.getGenerativeSummary() != null) {
            var gs = p.getGenerativeSummary();
            if (gs.getDescription() != null && nonEmpty(gs.getDescription().getText())) {
                return gs.getDescription().getText();
            }
            if (gs.getOverview() != null && nonEmpty(gs.getOverview().getText())) {
                return gs.getOverview().getText();
            }
        }
        if (p.getAreaSummary() != null && nonEmpty(p.getAreaSummary().getText())) {
            return p.getAreaSummary().getText();
        }
        // 카드에서는 허용
        if (p.getAddressDescriptor() != null) {
            String ad = summarizeAddressDescriptor(
                    p.getAddressDescriptor().getLandmarks(),
                    p.getAddressDescriptor().getAreas(),
                    false
            );
            if (nonEmpty(ad)) return ad;
        }
        return null;
    }

    // 상세: 문단형 텍스트만 허용 — addressDescriptor는 제외
    private static String pickSummaryFromDetailsStrict(PlacesV1PlaceDetailsResponse d) {
        if (d.getEditorialSummary() != null && nonEmpty(d.getEditorialSummary().getOverview())) {
            return d.getEditorialSummary().getOverview();
        }
        if (d.getGenerativeSummary() != null) {
            var gs = d.getGenerativeSummary();
            if (gs.getDescription() != null && nonEmpty(gs.getDescription().getText())) {
                return gs.getDescription().getText();
            }
            if (gs.getOverview() != null && nonEmpty(gs.getOverview().getText())) {
                return gs.getOverview().getText();
            }
        }
        if (d.getAreaSummary() != null && nonEmpty(d.getAreaSummary().getText())) {
            return d.getAreaSummary().getText();
        }
        // addressDescriptor(“~ 인근/일대”)는 상세에서 사용 금지
        return null;
    }

    private static String summarizeAddressDescriptor(List<?> landmarks, List<?> areas, boolean isDetail) {
        String lmName = extractDisplayNameText(landmarks);
        if (nonEmpty(lmName)) return lmName + " 인근";
        String areaName = extractDisplayNameText(areas);
        if (nonEmpty(areaName)) return areaName + " 일대";
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String extractDisplayNameText(List<?> items) {
        if (items == null || items.isEmpty()) return null;
        Object first = items.get(0);
        try {
            var disp = first.getClass().getMethod("getDisplayName").invoke(first);
            if (disp == null) return null;
            var text = disp.getClass().getMethod("getText").invoke(disp);
            return text != null ? text.toString() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean nonEmpty(String s) { return s != null && !s.isBlank(); }
    private static <T> T firstNonNull(T a, T b) { return (a != null) ? a : b; }
}
