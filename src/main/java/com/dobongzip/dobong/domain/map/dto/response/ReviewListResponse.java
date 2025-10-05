package com.dobongzip.dobong.domain.map.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class ReviewListResponse {
    private String placeId;
    private Double rating;          // 평균 별점
    private Integer reviewCount;    // 전체 리뷰 수
    private List<Review> reviews;   // 상위 N개

    @Getter
    @Builder
    public static class Review {
        private String authorName;         // 작성자 이름(있을 때)
        private String authorProfilePhoto; // 아바타 URL(있을 때)
        private Double rating;             // 별점
        private String text;               // 본문
        private String relativeTime;       // "1시간 전" 등
        private boolean isMine;
    }
}
