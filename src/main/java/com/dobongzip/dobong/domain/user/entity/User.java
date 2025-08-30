package com.dobongzip.dobong.domain.user.entity;

import com.dobongzip.dobong.global.security.dto.auth.request.ProfileRequestDto;
import com.dobongzip.dobong.global.security.enums.LoginType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;


    @Column(unique = true,name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "phoneNumber")
    private String phoneNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "gender")
    private String gender;

    @Column(name = "birth")
    private String birth;

    @Enumerated(EnumType.STRING)
    @Column(name = "loginType")
    private LoginType loginType; // APP / GOOGLE / KAKAO

    private boolean profileCompleted;

    public void updateProfile(ProfileRequestDto request) {
        this.name = request.getName();
        this.nickname = request.getNickname();
        this.gender = request.getGender();
        this.birth = request.getBirth();
        this.profileCompleted = true;
    }


    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
