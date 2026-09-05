package com.earthonline.backend.futureletters;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FutureLetterRepository
        extends JpaRepository<FutureLetterEntity, Long> {

    List<FutureLetterEntity> findAllByUserIdOrderByOpenAtAsc(Long userId);

    Optional<FutureLetterEntity> findByIdAndUserId(Long id, Long userId);

}
