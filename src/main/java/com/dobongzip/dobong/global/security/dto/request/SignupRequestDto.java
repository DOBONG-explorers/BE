package com.dobongzip.dobong.global.security.dto.request;

import lombok.Getter;

@Getter

public class SignupRequestDto {
    private String appId;
    private String password;
    private String phoneNumber;
    private String name;
    private String email;
    private String gender;
    private String birth;
    private int age;
}
