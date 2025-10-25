package com.dobongzip.dobong.domain.map.controller;

import com.dobongzip.dobong.domain.map.dto.request.ReviewCreateRequest;
import com.dobongzip.dobong.domain.map.dto.request.ReviewUpdateRequest;
import com.dobongzip.dobong.domain.map.dto.response.PlaceDetailsResponse;
import com.dobongzip.dobong.domain.map.dto.response.PlaceDto;
import com.dobongzip.dobong.domain.map.dto.response.ReviewIdResponse;
import com.dobongzip.dobong.domain.map.dto.response.ReviewListResponse;
import com.dobongzip.dobong.domain.map.service.PlaceService;
import com.dobongzip.dobong.domain.map.service.ReviewService;
import com.dobongzip.dobong.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "맵 페이지", description ="도봉구 장소정보+리뷰 API")
@RequestMapping(value = "/api/v1/places", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PlaceController {

    private final PlaceService placeService;
    private final ReviewService reviewService;

    @Operation(
            summary = "도봉 명소 검색(목록/카드)",
            description = """
                    사용자 좌표 기준으로 도봉구 명소를 검색합니다.<br>
                    이름, 주소, 거리, 대표사진, 별점/리뷰수, 영업시간, 가격레벨 등이 포함됩니다.<br>
                    `lat`, `lng`: 사용자 현재 위도, 경도.<br>
                     limit는 반환 개수입니다.
                    """)
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

    @Operation(
            summary = "장소 상세 조회",
            description = "Wikipedia/Google 소개, 주소, 영업시간, 가격레벨, 전화, 사진들, 별점/리뷰수를 반환합니다.<br>" +
                    "`placeId`는 '/dobong' 명소 검색 API에서 받은 `placeId` 값을 사용합니다."
    )
    @GetMapping("/{placeId}")
    public ResponseEntity<CommonResponse<PlaceDetailsResponse>> getPlaceDetail(@PathVariable String placeId) {
        var dto = placeService.getPlaceDetail(placeId);
        return ResponseEntity.ok(CommonResponse.onSuccess(dto));
    }

    @Operation(
            summary = "리뷰 목록(로컬+구글 합산)",
            description = """
                    로컬(DB) 리뷰와 Google 리뷰를 합쳐 반환합니다.
                    로컬 리뷰 항목에는 isMine 플래그가 포함되어 로그인 사용자의 리뷰 여부를 구분할 수 있습니다.
                    비로그인 호출도 가능합니다.
                    """)
    @GetMapping("/{placeId}/reviews")
    public ResponseEntity<CommonResponse<ReviewListResponse>> getCombinedReviews(
            @PathVariable String placeId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        ReviewService.AvatarResolver avatar = (memberId) -> null; // TODO: 회원 프로필 URL 리졸버
        var dto = reviewService.getCombinedReviews(placeId, limit, false, avatar);
        return ResponseEntity.ok(CommonResponse.onSuccess(dto));
    }

    @Operation(
            summary = "리뷰 작성",
            description = "[로그인 필수] 해당 장소에 리뷰를 작성합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReviewCreateRequest.class),
                            examples = @ExampleObject(value = """
                            { "rating": 4.5, "text": "경치가 정말 좋아요!" }
                            """))
            ))
    @PostMapping("/{placeId}/reviews")
    public ResponseEntity<CommonResponse<?>> createReview(
            @PathVariable String placeId,
            @Valid @RequestBody ReviewCreateRequest req
    ) {
        Long id = reviewService.create(placeId, req); // 서비스가 401/권한/중복 처리
        return ResponseEntity.ok(CommonResponse.onSuccess(new ReviewIdResponse(id)));
    }

    @Operation(
            summary = "리뷰 수정",
            description = "[로그인 필수] 작성자 본인만 리뷰를 수정할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReviewUpdateRequest.class),
                            examples = @ExampleObject(value = """
                            { "rating": 4.0, "text": "재방문 후 수정: 야간 풍경이 더 좋아요." }
                            """))))
    @PutMapping("/{placeId}/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<?>> updateReview(
            @PathVariable String placeId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest req
    ) {
        reviewService.update(placeId, reviewId, req);
        return ResponseEntity.ok(CommonResponse.onSuccess("UPDATED"));
    }

    @Operation(
            summary = "리뷰 삭제(소프트)",
            description = "[로그인 필수] 작성자 본인만 리뷰를 삭제할 수 있습니다. 소프트 삭제 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{placeId}/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<?>> deleteReview(
            @PathVariable String placeId,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(placeId, reviewId);
        return ResponseEntity.ok(CommonResponse.onSuccess("DELETED"));
    }
}
