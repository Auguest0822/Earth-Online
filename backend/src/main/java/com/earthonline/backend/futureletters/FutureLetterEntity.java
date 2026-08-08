package com.earthonline.backend.futureletters;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "future_letters")
public class FutureLetterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String mood;

    @Column(name = "open_at", nullable = false)
    private LocalDateTime openAt;

    @Column(nullable = false, length = 30)
    private String status = "sealed";

    @Column(nullable = false, length = 30)
    private String visibility = "private";

    @Column(name = "allow_edit_before_open", nullable = false)
    private Boolean allowEditBeforeOpen = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FutureLetterEntity() {
    }

    public FutureLetterEntity(
            Long userId,
            String title,
            String content,
            String mood,
            LocalDateTime openAt,
            String status,
            String visibility,
            Boolean allowEditBeforeOpen,
            LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.openAt = openAt;
        this.status = status;
        this.visibility = visibility;
        this.allowEditBeforeOpen = allowEditBeforeOpen;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "sealed";
        }
        if (visibility == null) {
            visibility = "private";
        }
        if (allowEditBeforeOpen == null) {
            allowEditBeforeOpen = true;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public LocalDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(LocalDateTime openAt) {
        this.openAt = openAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Boolean getAllowEditBeforeOpen() {
        return allowEditBeforeOpen;
    }

    public void setAllowEditBeforeOpen(Boolean allowEditBeforeOpen) {
        this.allowEditBeforeOpen = allowEditBeforeOpen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}