package com.dobongzip.dobong.global.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.access.expiration}")
    private long accessTokenExpirationMs; // ex: 1000 * 60 * 60 (1시간)

    public String createAccessToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
}
