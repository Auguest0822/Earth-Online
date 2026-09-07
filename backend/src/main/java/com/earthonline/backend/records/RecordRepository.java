package com.earthonline.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface RecordRepository
        extends JpaRepository<RecordEntity, Long> {

    List<RecordEntity> findAllByUserId(Long userId);

    @Query(
            value = """
                    select record from RecordEntity record
                    where record.userId = :userId
                      and (:startDate is null or record.recordDate >= :startDate)
                      and (:endDate is null or record.recordDate <= :endDate)
                      and (:eventType is null or record.eventType = :eventType)
                      and (:tag is null or :tag member of record.keywords)
                    order by record.recordDate desc, record.createdAt desc
                    """,
            countQuery = """
                    select count(record) from RecordEntity record
                    where record.userId = :userId
                      and (:startDate is null or record.recordDate >= :startDate)
                      and (:endDate is null or record.recordDate <= :endDate)
                      and (:eventType is null or record.eventType = :eventType)
                      and (:tag is null or :tag member of record.keywords)
                    """
    )
    Page<RecordEntity> findPageByFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("eventType") String eventType,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query("select distinct year(record.recordDate) from RecordEntity record where record.userId = :userId order by year(record.recordDate) desc")
    List<Integer> findDistinctYearsByUserId(@Param("userId") Long userId);

    @Query("select distinct keyword from RecordEntity record join record.keywords keyword where record.userId = :userId order by keyword")
    List<String> findDistinctTagsByUserId(@Param("userId") Long userId);

    @Query("select distinct record.eventType from RecordEntity record where record.userId = :userId and record.eventType is not null order by record.eventType")
    List<String> findDistinctEventTypesByUserId(@Param("userId") Long userId);

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
