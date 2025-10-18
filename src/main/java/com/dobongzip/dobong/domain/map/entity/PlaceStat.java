package com.dobongzip.dobong.domain.map.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_top3")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceStat {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String placeId;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private LocalDateTime lastViewedAt;
}
