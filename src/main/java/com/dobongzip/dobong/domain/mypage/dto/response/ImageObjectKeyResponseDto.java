package com.dobongzip.dobong.domain.mypage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageObjectKeyResponseDto {
    private String objectKey;   // S3 임시 업로드 키만 반환
}
