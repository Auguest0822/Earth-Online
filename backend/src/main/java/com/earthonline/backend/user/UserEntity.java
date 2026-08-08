package com.earthonline.backend.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public UserEntity() {
    }

    public UserEntity(
            String username,
            String passwordHash,
            String email,
            LocalDateTime createdAt
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
