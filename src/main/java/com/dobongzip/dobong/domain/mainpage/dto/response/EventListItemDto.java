package com.dobongzip.dobong.domain.mainpage.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventListItemDto {
    private String id;        // 상세조회용 안정적 ID
    private String title;     // 행사명 (TITLE)
    private String dateText;  // 화면 표시용 날짜 문자열 (DATE 또는 START~END)
}


