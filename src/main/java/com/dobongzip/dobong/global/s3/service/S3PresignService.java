package com.dobongzip.dobong.global.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}") private String bucket;
    @Value("${aws.s3.key-prefix}") private String keyPrefix; // 기본 uploads/ 로 고정
    @Value("${aws.s3.presign-exp-minutes}") private int expMinutes;

    private static final Set<String> ALLOWED = Set.of(MediaType.IMAGE_PNG_VALUE);

    /** PNG 전용 Presigned PUT URL 생성 */
    public PresignResult createUserPngPutUrl(Long userId, String originalFilename, Long contentLength) {
        final String contentType = MediaType.IMAGE_PNG_VALUE;
        if (!ALLOWED.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported contentType: " + contentType);
        }

        String safe = sanitize(originalFilename);
        if (!safe.toLowerCase().endsWith(".png")) safe = safe + ".png";

        LocalDate d = LocalDate.now();

        // 임시 키: uploads/{userId}/tmp/YYYY/MM/{uuid}_{safe}.png
        String key = normalize("%s%d/tmp/%d/%02d/%s_%s".formatted(
                ensurePrefix(keyPrefix), userId, d.getYear(), d.getMonthValue(), UUID.randomUUID(), safe
        ));

        PutObjectRequest.Builder pob = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType);
        if (contentLength != null && contentLength > 0) {
            pob = pob.contentLength(contentLength);
        }
        PutObjectRequest put = pob.build();

        PresignedPutObjectRequest pre = presigner.presignPutObject(p ->
                p.signatureDuration(Duration.ofMinutes(expMinutes))
                        .putObjectRequest(put)
        );

        URL url = pre.url();
        Map<String, List<String>> headers = pre.signedHeaders(); // 다중값 헤더

        return new PresignResult(url.toString(), "PUT", headers, key);
    }

    // keyPrefix가 비어 있어도 "uploads/" 기본 보장
    private static String ensurePrefix(String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "uploads/" : prefix;
        return p.endsWith("/") ? p : p + "/";
    }

    // 키 내 중복 슬래시 정리
    private static String normalize(String key) {
        return key.replaceAll("/{2,}", "/");
    }

    private static String sanitize(String name) {
        if (name == null) return "file";
        String base = name.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}._-]", "_");
        return base.isBlank() ? "file" : base;
    }

    public record PresignResult(
            String url,
            String method,
            Map<String, List<String>> headers,
            String key
    ) {}
}
