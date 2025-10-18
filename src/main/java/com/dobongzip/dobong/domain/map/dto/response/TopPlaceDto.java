package com.dobongzip.dobong.domain.map.dto.response;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TopPlaceDto {
    private String placeId;
    private String name;
    private String address;

    private Double latitude;
    private Double longitude;

    private Long distanceMeters;
    private String distanceText;

    private String imageUrl;
    private String phone;

    private Double rating;
    private Integer reviewCount;
}
