package com.dobongzip.dobong.domain.map.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PlaceDetailsResponse {
    private String placeId;
    private String name;
    private String address;
    private String description;        // editorial → generative 순 폴백
    private List<String> openingHours; // 요일별 이용시간
    private Integer priceLevel;        // 이용금액 레벨(0~4)
    private String phone;              // 국제/국내
    private Double rating;             // 평균 별점
    private Integer reviewCount;       // 리뷰 수
    private List<String> photos;       // 여러 장
    private Location location;
    private boolean liked;
    @Getter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;
    }
}
