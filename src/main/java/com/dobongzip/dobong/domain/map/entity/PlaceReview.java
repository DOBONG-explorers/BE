package com.dobongzip.dobong.domain.map.entity;

import com.dobongzip.dobong.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "place_review",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_place_user", columnNames = {"place_id", "user_id"})
        },
        indexes = {
                @Index(name = "ix_place_review_place", columnList = "place_id, deleted, created_at"),
                @Index(name = "ix_place_review_user",  columnList = "user_id")
        }
)
public class PlaceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // ← DB 컬럼명과 일치시킴
    private Long id;

    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",           // ← 명시해 주면 더 안전
            foreignKey = @ForeignKey(name = "fk_place_review_user")
    )
    private User author;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "text")
    private String text;

    @Column(nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 편의 메서드
    public Long getAuthorId() { return author != null ? author.getId() : null; }

    public void edit(Double rating, String text, String editorName) {
        this.rating = rating;
        this.text = text;
        this.authorName = editorName;
    }

    public void softDelete() { this.deleted = true; }
}
