package com.dobongzip.dobong.domain.like.repository;

import com.dobongzip.dobong.domain.like.entity.PlaceLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {
    boolean existsByUser_IdAndPlaceId(Long userId, String placeId);
    void deleteByUser_IdAndPlaceId(Long userId, String placeId);
    List<PlaceLike> findByUser_Id(Long userId, Pageable pageable);
}
