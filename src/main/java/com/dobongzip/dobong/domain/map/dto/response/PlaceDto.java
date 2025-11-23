package com.dobongzip.dobong.domain.map.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class PlaceDto {
    private String placeId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    private long distanceMeters;
    private String distanceText;

    private String imageUrl;
    private String description;       // (요약 있을 때)
    private List<String> openingHours;
    private Integer priceLevel;
    private String mapsUrl;
    private String phone;

    // 👇 추가
    private Double rating;            // 평균 별점
    private Integer reviewCount;      // 리뷰 수
}
