package com.dobongzip.dobong.domain.map.controller;

import com.dobongzip.dobong.domain.map.dto.response.PlaceDetailsResponse;
import com.dobongzip.dobong.domain.map.dto.response.PlaceDto;
import com.dobongzip.dobong.domain.map.dto.response.ReviewListResponse;
import com.dobongzip.dobong.domain.map.service.PlaceService;
import com.dobongzip.dobong.global.response.CommonResponse;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/places", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PlaceController {

    private final PlaceService placeService;

    /** ① 검색(목록/카드) — 이름, 별점, 리뷰수, 거리, 전화(있으면), 대표사진 1장 */
    // 예: GET /api/v1/places/dobong?lat=37.665&lng=127.043&limit=10
    @GetMapping("/dobong")
    public ResponseEntity<CommonResponse<List<PlaceDto>>> getDobong(
            @RequestParam @DecimalMin("-90.0")  @DecimalMax("90.0")  double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lng,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int limit
    ) {
        int capped = Math.max(1, Math.min(limit, 30));
        var list = placeService.findDobongAttractions(lat, lng, capped);
        return ResponseEntity.ok(CommonResponse.onSuccess(list));
    }

    /** ② 상세 — 장소 소개(에디토리얼/제너러티브), 주소, 이용시간, 이용금액(가격레벨), 사진들, 전화, 별점/리뷰수 */
    // 예: GET /api/v1/places/{placeId}
    @GetMapping("/{placeId}")
    public ResponseEntity<CommonResponse<PlaceDetailsResponse>> getPlaceDetail(@PathVariable String placeId) {
        var dto = placeService.getPlaceDetail(placeId);
        return ResponseEntity.ok(CommonResponse.onSuccess(dto));
    }

    /** ③ 리뷰 목록 — 평균 별점, 리뷰수, 리뷰 리스트(작성자/별점/본문/상대시간/아바타 등) */
    // 예: GET /api/v1/places/{placeId}/reviews?limit=20
    @GetMapping("/{placeId}/reviews")
    public ResponseEntity<CommonResponse<ReviewListResponse>> getPlaceReviews(
            @PathVariable String placeId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        var dto = placeService.getPlaceReviews(placeId, limit);
        return ResponseEntity.ok(CommonResponse.onSuccess(dto));
    }
}
