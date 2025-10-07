package com.dobongzip.dobong.global.security.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ProfileRequestDto {
    @NotBlank
    private String name;
    @NotBlank
    private String nickname;
    @NotBlank
    private String gender;
    @NotBlank
    private String birth; // yyyy-MM-dd 형식
}
