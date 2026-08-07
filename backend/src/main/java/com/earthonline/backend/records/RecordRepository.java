package com.earthonline.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRepository
        extends JpaRepository<RecordEntity, Long> {
}