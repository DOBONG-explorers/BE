package com.dobongzip.dobong.global.security.dto.auth.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OIDCRequestDto {
    private String idToken;
}