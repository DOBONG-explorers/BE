package com.dobongzip.dobong.global.security.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class GoogleOidcService {

    @Value("${google.web-client-id}")
    private String webClientId;             // aud 검증 대상

    @Value("${google.oidc.issuer}")
    private String issuer;                  // https://accounts.google.com

    @Value("${google.oidc.jwks-uri}")
    private String jwksUri;                 // https://www.googleapis.com/oauth2/v3/certs

    public GoogleClaims verify(String idToken) { return verify(idToken, null); }

    public GoogleClaims verify(String idToken, @Nullable String expectedNonce) {
        try {
            ConfigurableJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
            var jwkSource = new RemoteJWKSet<SecurityContext>(new URL(jwksUri));
            p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));

            JWTClaimsSet c = p.process(idToken, null);

            // 표준 검증
            if (!issuer.equals(c.getIssuer())) throw new IllegalArgumentException("bad iss");
            if (c.getAudience() == null || !c.getAudience().contains(webClientId)) throw new IllegalArgumentException("bad aud");
            if (new Date().after(c.getExpirationTime())) throw new IllegalArgumentException("expired");
            if (expectedNonce != null && !expectedNonce.equals(c.getStringClaim("nonce")))
                throw new IllegalArgumentException("bad nonce");

            String sub = c.getSubject();                        // 고유 사용자 ID
            String email = (String) c.getClaim("email");        // email 권한 필수라면 null이면 거절
            Boolean emailVerified = (Boolean) c.getClaim("email_verified"); // 있을 때만 사용

            return new GoogleClaims(sub, email, emailVerified);
        } catch (Exception e) {
            throw new RuntimeException("Google OIDC verify failed", e);
        }
    }

    public record GoogleClaims(String sub, String email, Boolean emailVerified) {}
}
