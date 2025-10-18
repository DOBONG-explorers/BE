package com.dobongzip.dobong.domain.mainpage.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeritageDetailDto {
    private String id;
    private String name;            // 한글 명칭
    private String nameHanja;       // 한자/병기 명칭(있으면)
    private String address;         // 주소(도로명 우선 → 지번)
    private String designationNo;   // 지정번호
    private String designationDate; // 지정일자 (yyyy-MM-dd 형태로 정규화 시도)
    private String tel;             // 전화번호
    private String description;     // 유산 소개/해설
    private String imageUrl;        // 대표 이미지
    private Double lat;             // 위도
    private Double lng;             // 경도
}
