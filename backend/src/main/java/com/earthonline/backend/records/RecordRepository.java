package com.earthonline.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface RecordRepository
        extends JpaRepository<RecordEntity, Long> {

    List<RecordEntity> findAllByUserId(Long userId);

    List<RecordEntity> findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    Optional<RecordEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    @Query("""
            select distinct record from RecordEntity record
            left join record.keywords keyword
            where record.userId = :userId
              and (
                lower(record.title) like lower(concat('%', :query, '%'))
                or lower(record.content) like lower(concat('%', :query, '%'))
                or lower(record.eventType) like lower(concat('%', :query, '%'))
                or lower(keyword) like lower(concat('%', :query, '%'))
                or record.mood like concat('%', :query, '%')
              )
            order by record.recordDate desc, record.createdAt desc
            """)
    List<RecordEntity> searchByUser(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );
}
