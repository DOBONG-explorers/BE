package com.dobongzip.dobong.domain.mypage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponseDto {
    private String nickname;
    private String birth;
    private String email;
}
