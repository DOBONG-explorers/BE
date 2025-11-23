package com.dobongzip.dobong.global.security.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class KakaoOidcService {
    @Value("${kakao.native-app-key}") private String clientId;   // aud
    @Value("${kakao.oidc.issuer}")  private String issuer;     // https://kauth.kakao.com
    @Value("${kakao.oidc.jwks-uri}") private String jwksUri;   // https://kauth.kakao.com/.well-known/jwks.json

    public KakaoClaims verify(String idToken) {
        return verify(idToken, null);
    }

    public KakaoClaims verify(String idToken, String expectedNonce) {
        try {
            var proc = new DefaultJWTProcessor<SecurityContext>();
            var jwkSource = new RemoteJWKSet<SecurityContext>(new URL(jwksUri));
            proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));

            JWTClaimsSet c = proc.process(idToken, null);

            if (!issuer.equals(c.getIssuer())) throw new IllegalArgumentException("bad iss");
            if (c.getAudience() == null || !c.getAudience().contains(clientId)) throw new IllegalArgumentException("bad aud");
            if (new Date().after(c.getExpirationTime())) throw new IllegalArgumentException("expired");
            if (expectedNonce != null && !expectedNonce.equals(c.getStringClaim("nonce"))) throw new IllegalArgumentException("bad nonce");

            String sub   = c.getSubject();
            String email = (String) c.getClaim("email");          // 콘솔에서 필수 동의
            Boolean emailVerified = (Boolean) c.getClaim("email_verified"); // 있을 때만 사용

            return new KakaoClaims(sub, email, emailVerified);
        } catch (Exception e) {
            throw new RuntimeException("Kakao OIDC verify failed", e);
        }
    }

    public record KakaoClaims(String sub, String email, Boolean emailVerified) {}
}
