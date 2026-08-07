package com.earthonline.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository
        extends JpaRepository<RecordEntity, Long> {

    List<RecordEntity> findAllByUserId(Long userId);

    Optional<RecordEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
