package com.dobongzip.dobong.domain.like.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeCardResponse {
    private String placeId;
    private String name;
    private String imageUrl;
}
