package com.dobongzip.dobong.domain.like.controller;

import com.dobongzip.dobong.domain.like.dto.response.LikeCardResponse;
import com.dobongzip.dobong.domain.like.service.LikeService;
import com.dobongzip.dobong.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "좋아요 페이지")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/places")
public class PlaceLikeController {

    private final LikeService likeService;

    @Operation(summary = "장소 좋아요")
    @PostMapping("/{placeId}/like")
    public ResponseEntity<CommonResponse<?>> like(@PathVariable String placeId) {
        likeService.like(placeId);
        return ResponseEntity.ok(CommonResponse.onSuccess("LIKED"));
    }

    @Operation(summary = "장소 좋아요 취소")
    @DeleteMapping("/{placeId}/like")
    public ResponseEntity<CommonResponse<?>> unlike(@PathVariable String placeId) {
        likeService.unlike(placeId);
        return ResponseEntity.ok(CommonResponse.onSuccess("UNLIKED"));
    }

    @Operation(
            summary = "내가 좋아요한 목록(장소명+이미지)",
            description = """
            로그인 사용자가 좋아요한 장소 카드를 반환합니다.
            - 정렬: `order=latest`(최신순, 기본값) 또는 `order=oldest`(오래된순)
            - 개수: `size`는 1~50까지""")
    @GetMapping("/likes/me")
    public ResponseEntity<CommonResponse<List<LikeCardResponse>>> myLikes(
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "latest") String order // latest | oldest
    ) {
        var list = likeService.myLikes(size, order);
        return ResponseEntity.ok(CommonResponse.onSuccess(list));
    }
}
