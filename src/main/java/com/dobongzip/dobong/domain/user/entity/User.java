package com.dobongzip.dobong.domain.user.entity;

import com.dobongzip.dobong.global.security.enums.LoginType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appId;         // 아이디
    private String password;      // 비밀번호 (암호화된 상태)
    private String phoneNumber;

    private String name;
    private String email;
    private String gender;
    private String birth;
    private int age;

    @Enumerated(EnumType.STRING)
    private LoginType loginType; // APP / GOOGLE / KAKAO

}
