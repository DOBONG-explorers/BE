package com.dobongzip.dobong.domain.map.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "place_review")
@Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class PlaceReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String placeId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "text")
    private String text;

    @Column(nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void edit(Double rating, String text, String editorName) {
        this.rating = rating;
        this.text = text;
        this.authorName = editorName;
    }

    public void softDelete() { this.deleted = true; }
}