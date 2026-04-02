package com.example.health.care.Repository;

import com.example.health.care.Entity.HealthPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthPointsRepository extends JpaRepository<HealthPoints, Long> {

    Optional<HealthPoints> findByUserId(Long userId);

    // Leaderboard — top 10 by points
    @Query("SELECT h FROM HealthPoints h ORDER BY h.totalPoints DESC")
    List<HealthPoints> findTopByPoints();
}