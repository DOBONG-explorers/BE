package com.dobongzip.dobong.domain.like.service;

import com.dobongzip.dobong.domain.like.dto.response.LikeCardResponse;
import com.dobongzip.dobong.domain.like.entity.PlaceLike;
import com.dobongzip.dobong.domain.like.repository.PlaceLikeRepository;
import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final PlaceLikeRepository placeLikeRepository;
    private final GooglePlacesClientV1 v1;
    private final AuthenticatedProvider authenticatedProvider;

    /** 좋아요(멱등) */
    public void like(String placeId) {
        if (!authenticatedProvider.isAuthenticated()) {
            throw BusinessException.of(StatusCode.LOGIN_REQUIRED);
        }
        User me = authenticatedProvider.getCurrentUser(); // 비로그인 → 401
        Long uid = me.getId();

        if (placeLikeRepository.existsByUser_IdAndPlaceId(uid, placeId)) return;

        // 스냅샷(장소명/대표사진) — 없어도 동작
        var d = v1.fetchPlaceDetails(placeId);
        String name = (d != null && d.getDisplayName()!=null) ? d.getDisplayName().getText() : null;
        String photoName = (d != null && d.getPhotos()!=null && !d.getPhotos().isEmpty())
                ? d.getPhotos().get(0).getName() : null;

        try {
            placeLikeRepository.save(new PlaceLike(me, placeId, name, photoName)); // ★ 엔티티 직접 연결
        } catch (DataIntegrityViolationException ignored) {
            // 동시성으로 unique 위반 시 무시 (멱등 보장)
        }
    }

    /** 좋아요 취소(멱등) */
    public void unlike(String placeId) {
        if (!authenticatedProvider.isAuthenticated()) {
            throw BusinessException.of(StatusCode.LOGIN_REQUIRED);
        }
        User me = authenticatedProvider.getCurrentUser();
        placeLikeRepository.deleteByUser_IdAndPlaceId(me.getId(), placeId);
    }

    /** 현재 로그인 사용자의 liked 여부 (비로그인=false) */
    @Transactional(readOnly = true)
    public boolean isLikedForCurrentUser(String placeId) {
        Long uid = authenticatedProvider.currentUserIdOrNull();
        return uid != null && placeLikeRepository.existsByUser_IdAndPlaceId(uid, placeId);
    }

    /** 내가 좋아요한 카드 목록 (장소명+이미지) */
    @Transactional(readOnly = true)
    public List<LikeCardResponse> myLikes(int size, String order) {
        if (!authenticatedProvider.isAuthenticated()) {
            throw BusinessException.of(StatusCode.LOGIN_REQUIRED);
        }
        User me = authenticatedProvider.getCurrentUser();
        int capped = Math.max(1, Math.min(size, 50));

        boolean oldest = "oldest".equalsIgnoreCase(order) || "asc".equalsIgnoreCase(order);
        // createdAt이 있으면 "createdAt", 없으면 "id"로 바꾸세요.
        Sort sort = oldest ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        var rows = placeLikeRepository.findByUser_Id(me.getId(), PageRequest.of(0, capped, sort));

        return rows.stream().map(r -> LikeCardResponse.builder()
                        .placeId(r.getPlaceId())
                        .name(r.getPlaceName())
                        .imageUrl(v1.buildPhotoUrl(r.getPhotoName(), 800))
                        .build())
                .toList();
    }
}
