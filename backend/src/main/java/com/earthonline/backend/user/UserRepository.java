package com.earthonline.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPublicId(String publicId);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByPublicId(String publicId);
}
