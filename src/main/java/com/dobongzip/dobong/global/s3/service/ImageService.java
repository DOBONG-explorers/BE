package com.dobongzip.dobong.global.s3.service;

import com.dobongzip.dobong.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final S3Client s3;
    private final Region region;

    @Value("${aws.s3.bucket}") private String bucket;
    @Value("${aws.s3.key-prefix}") private String keyPrefix; // 기본 uploads/
    @Value("${app.default-profile-image}") private String defaultProfileImageUrl;

    /** 프로필 이미지 최종 반영: tmp → final 복사, 이전 이미지 정리 */
    @Transactional
    public FinalizeResult finalizeProfileImage(User user, String uploadedKey) {
        if (uploadedKey == null || uploadedKey.isBlank()) {
            String keptKey = user.getProfileImageKey();
            return new FinalizeResult(keptKey, resolveUrl(keptKey));
        }

        // tmp 존재 확인
        s3.headObject(b -> b.bucket(bucket).key(uploadedKey));

        // 최종 키: uploads/{userId}/images/{uuid}.png
        String finalKey = normalize("%s%d/images/%s.png".formatted(
                ensurePrefix(keyPrefix), user.getId(), UUID.randomUUID()
        ));

        // tmp → final 복사
        s3.copyObject(b -> b
                .sourceBucket(bucket).sourceKey(uploadedKey)
                .destinationBucket(bucket).destinationKey(finalKey)
                .metadataDirective(MetadataDirective.COPY));

        // 기존 키 교체 & 정리
        String oldKey = user.getProfileImageKey();
        user.setProfileImageKey(finalKey);

        // tmp 삭제 (실패 무시)
        try { s3.deleteObject(b -> b.bucket(bucket).key(uploadedKey)); } catch (Exception ignored) {}

        // 예전 final 삭제 (실패 무시)
        if (oldKey != null && !oldKey.isBlank() && !oldKey.equals(finalKey)) {
            try { s3.deleteObject(b -> b.bucket(bucket).key(oldKey)); } catch (Exception ignored) {}
        }

        return new FinalizeResult(finalKey, s3PublicUrl(finalKey));
    }

    /** 프로필 이미지 제거(기본 이미지로 회귀) */
    @Transactional
    public void removeProfileImage(User user) {
        String oldKey = user.getProfileImageKey();
        user.setProfileImageKey(null);
        if (oldKey != null && !oldKey.isBlank()) {
            try { s3.deleteObject(b -> b.bucket(bucket).key(oldKey)); } catch (Exception ignored) {}
        }
    }

    /** 현재 보이는 URL (키 없으면 기본 이미지 URL 또는 규칙 URL) */
    public String resolveUrl(String key) {
        if (key == null || key.isBlank()) {
            if (defaultProfileImageUrl == null || defaultProfileImageUrl.isBlank()) {
                // 기본값이 비어 있으면 버킷 내 고정 경로 사용
                return s3PublicUrl(normalize("%sdefaults/default_profile.png".formatted(ensurePrefix(keyPrefix))));
            }
            return defaultProfileImageUrl;
        }
        return s3PublicUrl(key);
    }

    private String s3PublicUrl(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region.id(), key);
    }

    // (선택) 서버에서 직접 PNG 업로드가 필요할 때만 사용
    public String createUserTmpPngKey(Long userId) {
        LocalDate d = LocalDate.now();
        String key = "%s%d/tmp/%d/%02d/%s.png".formatted(ensurePrefix(keyPrefix), userId, d.getYear(), d.getMonthValue(), UUID.randomUUID());
        return normalize(key);
    }

    // (선택) 서버 업로드(프리사인 미사용 시)
    public void putPngObject(String key, MultipartFile file) {
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/png")
                    .build();
            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("S3 PNG 업로드 실패", e);
        }
    }

    private static String ensurePrefix(String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "uploads/" : prefix;
        return p.endsWith("/") ? p : p + "/";
    }

    private static String normalize(String key) {
        return key.replaceAll("/{2,}", "/");
    }

    public static class FinalizeResult {
        private final String key;
        private final String url;
        public FinalizeResult(String key, String url) { this.key = key; this.url = url; }
        public String getKey() { return key; }
        public String getUrl() { return url; }
    }
}
