package com.dobongzip.dobong.domain.mypage.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageFinalizeRequestDto {
    private String objectKey; // ← uploadedKey 대신 이름만 명확히 변경
}
