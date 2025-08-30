package com.dobongzip.dobong.global.security.dto.auth.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KakaoAuthRequestDto {
    private String idToken;
}