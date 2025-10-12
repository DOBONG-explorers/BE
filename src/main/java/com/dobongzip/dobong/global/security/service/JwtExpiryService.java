package com.dobongzip.dobong.global.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtExpiryService {
    @Value("${spring.jwt.secret}")
    private String secret;

    public Instant getExpiration(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody();
        Date exp = claims.getExpiration();
        return exp.toInstant();
    }
}
