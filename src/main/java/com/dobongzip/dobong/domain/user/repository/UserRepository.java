package com.dobongzip.dobong.domain.user.repository;

import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.global.security.enums.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailAndLoginType(String email, LoginType loginType);
    Optional<User> findByEmailAndLoginType(String email, LoginType loginType);

    // 👉 비번 재설정용
    Optional<User> findByEmail(String email);
}
