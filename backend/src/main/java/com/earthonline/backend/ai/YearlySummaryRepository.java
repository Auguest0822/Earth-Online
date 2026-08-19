package com.earthonline.backend.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface YearlySummaryRepository extends JpaRepository<YearlySummaryEntity, Long> {
    Optional<YearlySummaryEntity> findByUserIdAndYear(Long userId, Integer year);
}
