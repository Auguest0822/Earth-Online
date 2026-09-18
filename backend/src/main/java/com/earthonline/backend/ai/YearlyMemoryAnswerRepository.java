package com.earthonline.backend.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface YearlyMemoryAnswerRepository extends JpaRepository<YearlyMemoryAnswerEntity, Long> {
    /** 一个用户某一年最多一条缓存，所以用 Optional 而不是 List。 */
    Optional<YearlyMemoryAnswerEntity> findByUserIdAndYear(Long userId, Integer year);
}
