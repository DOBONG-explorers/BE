package com.dobongzip.dobong.domain.mainpage.controller;

import com.dobongzip.dobong.domain.mainpage.dto.request.EventSearchRequest;
import com.dobongzip.dobong.domain.mainpage.dto.response.*;
import com.dobongzip.dobong.domain.mainpage.service.MainService;
import com.dobongzip.dobong.domain.map.dto.response.TopPlaceDto;
import com.dobongzip.dobong.domain.map.service.PlaceService;
import com.dobongzip.dobong.global.response.CommonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "메인페이지", description = "메인페이지 조회 API")
@RestController
@RequestMapping("/api/v1/mainpage")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;
    private final PlaceService placeService;
    @Operation(
            summary = "도봉구 행사 이미지 리스트",
            description = "도봉구 문화행사 중 이미지(MAIN_IMG)가 있는 항목만 반환합니다.<br>" +
                    "`date` (YYYY-MM-DD): 특정 날짜 기준으로 조회. (없으면 현재 날짜 기준)"
    )
    @GetMapping("/dobong/images")
    public ResponseEntity<CommonResponse<List<EventImageDto>>> getDobongEventImages(
            @RequestParam(required = false) String date
    ) {
        EventSearchRequest req = new EventSearchRequest();
        req.setDate(date);
        return ResponseEntity.ok(CommonResponse.onSuccess(mainService.getDobongEventImages(req)));
    }

    @Operation(
            summary = "도봉구 행사 이름+날짜 리스트",
            description = "행사명과 날짜만 포함된 간단 리스트를 반환합니다.<br>" +
                    "`date` (YYYY-MM-DD): 특정 날짜 기준으로 조회. (없으면 현재 날짜 기준)"
    )
    @GetMapping("/dobong/list")
    public ResponseEntity<CommonResponse<List<EventListItemDto>>> listDobongEvents(
            @RequestParam(required = false) String date
    ) {
        EventSearchRequest req = new EventSearchRequest();
        req.setDate(date);
        return ResponseEntity.ok(CommonResponse.onSuccess(mainService.listDobongEvents(req)));
    }

    @Operation(
            summary = "도봉구 행사 상세 조회",
            description = "목록에서 받은 id로 단일 행사 정보를 반환합니다."
    )
    @GetMapping("/dobong/{id}")
    public ResponseEntity<CommonResponse<EventDto>> getDobongEventDetail(
            @PathVariable String id,
            @RequestParam(required = false) String date
    ) {
        EventSearchRequest req = new EventSearchRequest();
        req.setDate(date);
        return ResponseEntity.ok(CommonResponse.onSuccess(mainService.getDobongEventDetailRaw(id, req)));
    }

    @Operation(
            summary = "도봉구 문화유산 목록(사진+이름)",
            description = "도봉구 문화유산을 사진과 이름만 묶어 간단 목록으로 반환합니다. 각 항목의 id는 상세조회용 식별자입니다."
    )
    @GetMapping("/heritage/list")
    public ResponseEntity<CommonResponse<List<HeritageListItemDto>>> listHeritage() {
        return ResponseEntity.ok(CommonResponse.onSuccess(mainService.listDobongHeritage()));
    }

    @Operation(
            summary = "도봉구 문화유산 상세조회",
            description = "목록에서 받은 id로 단일 문화유산의 상세 정보를 '원본 JSON' 그대로 반환합니다."
    )
    @GetMapping("/heritage/{id}")
    public ResponseEntity<CommonResponse<JsonNode>> getHeritage(@PathVariable String id) {
        return ResponseEntity.ok(
                CommonResponse.onSuccess(mainService.getDobongHeritageDetailRaw(id))
        );
    }

    @Operation(
            summary = "인기 TOP 장소",
            description = "<b>정렬 기준:</b> '맵 페이지'에서 상세 조회(클릭)가 많이 발생한 순서입니다.<br>\n" +
                    "                    - <b>`lat`, `lng`:</b> 사용자 현재 위치 (필수). (거리 계산용)<br>\n" +
                    "                    - <b>`limit`:</b> 반환할 개수 (기본 3개)"
    )
    @GetMapping("/top")
    public ResponseEntity<CommonResponse<List<TopPlaceDto>>> getTopPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3") int limit
    ) {
        var list = placeService.getTopPlaces(lat, lng, limit);
        return ResponseEntity.ok(CommonResponse.onSuccess(list));
    }

    @Operation(
            summary = "랜덤으로 도봉구 느좋+핫플 장소 반환",
            description = "도봉구의 느좋+핫플 장소를 랜덤으로 하나 반환합니다. 사용자 위치(lat, lon)를 기준으로 거리를 계산합니다."
    )
    @GetMapping("/random-place")
    public ResponseEntity<CommonResponse<TopPlaceDto>> getRandomPlace(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        // 서비스 호출 시 사용자 위치를 함께 전달
        TopPlaceDto randomPlace = mainService.getRandomPlaceFromJson(lat, lon);
        return ResponseEntity.ok(CommonResponse.onSuccess(randomPlace));
    }
}
