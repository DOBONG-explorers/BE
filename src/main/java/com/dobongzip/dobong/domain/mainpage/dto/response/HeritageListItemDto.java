package com.dobongzip.dobong.domain.mainpage.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeritageListItemDto {
    private String id;          // 상세조회용 안정적 ID (이름+주소 해시)
    private String name;        // 문화유산명
    private String imageUrl;    // 썸네일(= 구글 places 혹은 placeholder)
}
