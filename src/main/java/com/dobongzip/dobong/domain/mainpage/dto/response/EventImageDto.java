package com.dobongzip.dobong.domain.mainpage.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventImageDto {
    private String id;
    private String imageUrl; // MAIN_IMG 그대로
}