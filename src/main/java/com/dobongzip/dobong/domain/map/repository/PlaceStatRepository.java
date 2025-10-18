package com.dobongzip.dobong.domain.map.repository;

import com.dobongzip.dobong.domain.map.entity.PlaceStat;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceStatRepository extends JpaRepository<PlaceStat, String> {

    @Query("select ps from PlaceStat ps order by ps.viewCount desc, ps.lastViewedAt desc")
    Page<PlaceStat> findTop(Pageable pageable);
}
