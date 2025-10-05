package com.dobongzip.dobong.domain.map.repository;

import com.dobongzip.dobong.domain.map.entity.PlaceReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    @Query("""
      select r from PlaceReview r
      where r.placeId = :placeId and r.deleted = false
      order by r.createdAt desc
    """)
    List<PlaceReview> findRecentByPlace(@Param("placeId") String placeId, Pageable pageable);

    @Query("""
      select avg(r.rating) from PlaceReview r
      where r.placeId = :placeId and r.deleted = false
    """)
    Double avgRating(@Param("placeId") String placeId);

    @Query("""
      select count(r) from PlaceReview r
      where r.placeId = :placeId and r.deleted = false
    """)
    long countByPlace(@Param("placeId") String placeId);

    Optional<PlaceReview> findByIdAndDeletedFalse(Long id);

    Optional<PlaceReview> findByPlaceIdAndAuthorIdAndDeletedFalse(String placeId, Long authorId);

    boolean existsByPlaceIdAndAuthorIdAndDeletedFalse(String placeId, Long authorId);
}
