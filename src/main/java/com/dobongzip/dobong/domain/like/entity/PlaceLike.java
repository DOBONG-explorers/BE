package com.dobongzip.dobong.domain.like.entity;

import com.dobongzip.dobong.domain.common.BaseEntity;
import com.dobongzip.dobong.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "place_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_place", columnNames = {"user_id", "place_id"}
        )
)
public class PlaceLike extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;

    // 목록 카드용 스냅샷(선택)
    @Column(name = "place_name", length = 200)
    private String placeName;

    /** Google Photos의 photo name (v1 photos[*].name) */
    @Column(name = "photo_name", columnDefinition = "TEXT")
    private String photoName;

    public PlaceLike(User user, String placeId, String placeName, String photoName) {
        this.user = user;
        this.placeId = placeId;
        this.placeName = placeName;
        this.photoName = photoName;
    }
}
