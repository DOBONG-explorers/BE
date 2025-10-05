package com.dobongzip.dobong.domain.map.service;

import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.domain.map.client.WikipediaClient;
import com.dobongzip.dobong.domain.map.dto.response.PlaceDetailsResponse;
import com.dobongzip.dobong.domain.map.dto.response.PlaceDto;
import com.dobongzip.dobong.domain.map.dto.response.PlacesV1PlaceDetailsResponse;
import com.dobongzip.dobong.domain.map.dto.response.PlacesV1SearchTextResponse;
import com.dobongzip.dobong.domain.map.dto.response.ReviewListResponse;
import com.dobongzip.dobong.domain.map.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final GooglePlacesClientV1 v1;
    private final WikipediaClient wikipedia;

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
                .build();
    }


    public ReviewListResponse getPlaceReviews(String placeId, int limit) {
        var d = v1.fetchPlaceReviews(placeId);
        if (d == null) return ReviewListResponse.builder()
                .placeId(placeId).rating(null).reviewCount(0).reviews(List.of()).build();

        var reviews = Optional.ofNullable(d.getReviews())
                .orElseGet(List::of).stream()
                .limit(limit)
                .map(r -> ReviewListResponse.Review.builder()
                        .authorName(r.getAuthorAttribution() != null ? r.getAuthorAttribution().getDisplayName() : null)
                        .authorProfilePhoto(r.getAuthorAttribution() != null ? r.getAuthorAttribution().getPhotoUri() : null)
                        .rating(r.getRating())
                        .text(r.getText() != null ? r.getText().getText() : null)
                        .relativeTime(r.getRelativePublishTimeDescription())
                        .build())
                .toList();

        return ReviewListResponse.builder()
                .placeId(d.getId())
                .rating(d.getRating())
                .reviewCount(d.getUserRatingCount())
                .reviews(reviews)
                .build();
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
