package com.dobongzip.dobong.domain.map.service;

import com.dobongzip.dobong.domain.map.client.GooglePlacesClientV1;
import com.dobongzip.dobong.domain.map.dto.request.ReviewCreateRequest;
import com.dobongzip.dobong.domain.map.dto.request.ReviewUpdateRequest;
import com.dobongzip.dobong.domain.map.dto.response.ReviewListResponse;
import com.dobongzip.dobong.domain.map.entity.PlaceReview;
import com.dobongzip.dobong.domain.map.repository.PlaceReviewRepository;
import com.dobongzip.dobong.domain.user.entity.User;
import com.dobongzip.dobong.global.exception.BusinessException;
import com.dobongzip.dobong.global.response.StatusCode;
import com.dobongzip.dobong.global.security.jwt.AuthenticatedProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final GooglePlacesClientV1 v1;                 // 구글 리뷰 조회용
    private final PlaceReviewRepository reviewRepository;  // 로컬 리뷰 CRUD용
    private final AuthenticatedProvider authenticatedProvider;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // ========= 조회 =========

    /** 구글 리뷰만(Read-only) */
    @Transactional(readOnly = true)
    public ReviewListResponse getGoogleReviews(String placeId, int limit) {
        var d = v1.fetchPlaceReviews(placeId);
        if (d == null) {
            return ReviewListResponse.builder()
                    .placeId(placeId).rating(null).reviewCount(0)
                    .reviews(List.of()).build();
        }

        var reviews = Optional.ofNullable(d.getReviews())
                .orElseGet(List::of).stream()
                .limit(clamp(limit, 1, 50))
                .map(r -> ReviewListResponse.Review.builder()
                        .authorName(r.getAuthorAttribution() != null ? r.getAuthorAttribution().getDisplayName() : null)
                        .authorProfilePhoto(r.getAuthorAttribution() != null ? r.getAuthorAttribution().getPhotoUri() : null)
                        .rating(r.getRating())
                        .text(r.getText() != null ? r.getText().getText() : null)
                        .relativeTime(r.getRelativePublishTimeDescription())
                        .isMine(false) // 구글 리뷰는 항상 false
                        .build())
                .toList();

        return ReviewListResponse.builder()
                .placeId(d.getId())
                .rating(d.getRating())
                .reviewCount(d.getUserRatingCount())
                .reviews(reviews)
                .build();
    }

    /** 로컬(DB) 리뷰만 */
    @Transactional(readOnly = true)
    public ReviewListResponse getLocalReviews(String placeId, int limit, AvatarResolver avatar) {
        Long me = authenticatedProvider.currentUserIdOrNull(); // 익명 가능

        var rows = reviewRepository.findRecentByPlace(placeId, PageRequest.of(0, clamp(limit, 1, 50)));

        var list = rows.stream()
                .map(r -> ReviewListResponse.Review.builder()
                        .authorName(r.getAuthorName())
                        .authorProfilePhoto(avatar != null ? avatar.photoUrlFor(r.getAuthorId()) : null)
                        .rating(r.getRating())
                        .text(r.getText())
                        .relativeTime(toRelative(r.getCreatedAt()))
                        .isMine(Objects.equals(r.getAuthorId(), me))
                        .build())
                .toList();

        Double avg = reviewRepository.avgRating(placeId);
        long cnt   = reviewRepository.countByPlace(placeId);

        return ReviewListResponse.builder()
                .placeId(placeId)
                .rating(avg == null ? null : roundHalf(avg))
                .reviewCount(Math.toIntExact(cnt))
                .reviews(list)
                .build();
    }

    /** 구글 + 로컬 합산(가중 평균) + 옵션: 내 리뷰 상단 고정 */
    @Transactional(readOnly = true)
    public ReviewListResponse getCombinedReviews(String placeId,
                                                 int limit,
                                                 boolean pinMineTop,
                                                 AvatarResolver avatar) {

        // 1) 구글 일부
        var g = getGoogleReviews(placeId, Math.min(10, limit));
        long gCnt = Optional.ofNullable(g.getReviewCount()).map(Integer::longValue).orElse(0L);
        Double gAvg = g.getRating();

        // 2) 로컬
        var localOnly = getLocalReviews(placeId, limit, avatar);
        long lCnt = Optional.ofNullable(localOnly.getReviewCount()).map(Integer::longValue).orElse(0L);
        Double lAvg = localOnly.getRating();

        // 3) 통계 합산
        Double combinedAvg = null;
        Integer combinedCnt = null;
        if ((gCnt + lCnt) > 0) {
            combinedAvg = ((gAvg == null ? 0.0 : gAvg) * gCnt + (lAvg == null ? 0.0 : lAvg) * lCnt) / (gCnt + lCnt);
            combinedAvg = roundHalf(combinedAvg);
            combinedCnt = Math.toIntExact(gCnt + lCnt);
        }

        // 4) 리스트 합치기(로컬 우선 + 구글 일부)
        var merged = new ArrayList<ReviewListResponse.Review>(localOnly.getReviews());
        merged.addAll(g.getReviews());

        // 5) 내 리뷰 상단 고정
        if (pinMineTop) {
            var user = authenticatedProvider.isAuthenticated() ? authenticatedProvider.getCurrentUser() : null;
            if (user != null) {
                var my = getMyReviewView(placeId,
                        user.getId(),
                        avatar != null ? avatar.photoUrlFor(user.getId()) : null,
                        displayNameOf(user));
                if (my != null) {
                    merged.removeIf(r ->
                            Objects.equals(r.getAuthorName(), my.getAuthorName()) &&
                                    Objects.equals(r.getText(), my.getText()) &&
                                    Objects.equals(r.getRating(), my.getRating())
                    );
                    merged.add(0, my);
                }
            }
        }

        return ReviewListResponse.builder()
                .placeId(placeId)
                .rating(combinedAvg != null ? combinedAvg : localOnly.getRating())
                .reviewCount(combinedCnt != null ? combinedCnt : localOnly.getReviewCount())
                .reviews(merged.stream().limit(clamp(limit, 1, 50)).toList())
                .build();
    }

    /** 내가 남긴 리뷰 1개(뷰용) */
    @Transactional(readOnly = true)
    public ReviewListResponse.Review getMyReviewView(String placeId, Long me, String myPhotoUrl, String myName) {
        if (me == null) return null;
        return reviewRepository.findByPlaceIdAndAuthorIdAndDeletedFalse(placeId, me)
                .map(r -> ReviewListResponse.Review.builder()
                        .authorName(r.getAuthorName())
                        .authorProfilePhoto(myPhotoUrl)
                        .rating(r.getRating())
                        .text(r.getText())
                        .relativeTime(toRelative(r.getCreatedAt()))
                        .isMine(true)
                        .build())
                .orElse(null);
    }

    // ========= CRUD(서비스에서 인증 처리) =========

    public Long create(String placeId, ReviewCreateRequest req) {
        var user = authenticatedProvider.getCurrentUser(); // 비로그인 시 내부에서 401 던짐
        Long me = user.getId();
        String myDisplayName = displayNameOf(user);

        if (reviewRepository.existsByPlaceIdAndAuthorIdAndDeletedFalse(placeId, me)) {
            throw BusinessException.of(StatusCode.REVIEW_ALREADY_EXISTS);
        }
        var saved = reviewRepository.save(PlaceReview.builder()
                .placeId(placeId)
                .authorId(me)
                .authorName(myDisplayName)
                .rating(req.rating())
                .text(req.text())
                .deleted(false)
                .build());
        return saved.getId();
    }

    public void update(String placeId, Long reviewId, ReviewUpdateRequest req) {
        var user = authenticatedProvider.getCurrentUser(); // 401 처리
        Long me = user.getId();
        String myDisplayName = displayNameOf(user);

        var r = reviewRepository.findByIdAndDeletedFalse(reviewId)
                .orElseThrow(() -> BusinessException.of(StatusCode.REVIEW_NOT_FOUND));
        if (!Objects.equals(r.getPlaceId(), placeId) || !Objects.equals(r.getAuthorId(), me)) {
            throw BusinessException.of(StatusCode.REVIEW_FORBIDDEN);
        }
        r.edit(req.rating(), req.text(), myDisplayName);
    }

    public void delete(String placeId, Long reviewId) {
        var user = authenticatedProvider.getCurrentUser(); // 401 처리
        Long me = user.getId();

        var r = reviewRepository.findByIdAndDeletedFalse(reviewId)
                .orElseThrow(() -> BusinessException.of(StatusCode.REVIEW_NOT_FOUND));
        if (!Objects.equals(r.getPlaceId(), placeId) || !Objects.equals(r.getAuthorId(), me)) {
            throw BusinessException.of(StatusCode.REVIEW_FORBIDDEN);
        }
        r.softDelete();
    }

    // ========= util =========

    public interface AvatarResolver { String photoUrlFor(Long memberId); }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(v, max)); }

    private static double roundHalf(double v) { return Math.round(v * 2) / 2.0; }

    private static String toRelative(java.time.LocalDateTime createdAt) {
        if (createdAt == null) return "-"; // ← 안전장치
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime then = createdAt.atZone(KST);
        var d = java.time.Duration.between(then, now);
        long s = d.getSeconds();
        if (s < 60) return "방금";
        long m = s/60; if (m < 60) return m + "분 전";
        long h = m/60; if (h < 24) return h + "시간 전";
        long day = h/24; if (day < 7) return day + "일 전";
        long w = day/7; if (w < 5) return w + "주 전";
        long mon = day/30; if (mon < 12) return mon + "개월 전";
        long y = day/365; return y + "년 전";
    }


    /** 표시 이름: 닉네임 > 이름 > 이메일 아이디 */
    private String displayNameOf(User u) {
        if (u == null) return "사용자";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        if (u.getName() != null && !u.getName().isBlank()) return u.getName();
        String email = u.getEmail();
        return (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "사용자";
    }
}
