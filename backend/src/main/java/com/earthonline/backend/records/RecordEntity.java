package com.earthonline.backend.records;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "records")
public class RecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(length = 32)
    private String mood;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "record_keywords", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "keyword", length = 30)
    private List<String> keywords = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "record_photos", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "photo_url", length = 1000)
    private List<String> photos = new ArrayList<>();

    public RecordEntity() {
    }

    public RecordEntity(
            String title,
            String content,
            LocalDateTime createdAt,
            Long userId,
            LocalDate recordDate,
            String mood,
            List<String> keywords,
            List<String> photos
    ) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.userId = userId;
        this.recordDate = recordDate;
        this.mood = mood;
        this.keywords = new ArrayList<>(keywords);
        this.photos = new ArrayList<>(photos);
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = new ArrayList<>(keywords); }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = new ArrayList<>(photos); }
}
