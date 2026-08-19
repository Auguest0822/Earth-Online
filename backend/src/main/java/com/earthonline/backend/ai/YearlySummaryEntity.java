package com.earthonline.backend.ai;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yearly_summaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_yearly_summary_user_year", columnNames = {"user_id", "summary_year"})
})
public class YearlySummaryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "summary_year", nullable = false) private Integer year;
    @Column(nullable = false, length = 200) private String title;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String summary;
    @Lob @Column(name = "mood_overview", nullable = false, columnDefinition = "TEXT") private String moodOverview;
    @Lob @Column(name = "keyword_insights_json", nullable = false, columnDefinition = "TEXT") private String keywordInsightsJson;
    @Lob @Column(name = "highlights_json", nullable = false, columnDefinition = "TEXT") private String highlightsJson;
    @Lob @Column(name = "letter_to_future", nullable = false, columnDefinition = "TEXT") private String letterToFuture;
    @Column(name = "record_count", nullable = false) private Integer recordCount;
    @Column(name = "source_hash", nullable = false, length = 64) private String sourceHash;
    @Column(nullable = false, length = 64) private String model;
    @Column(name = "prompt_version", nullable = false, length = 32) private String promptVersion;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected YearlySummaryEntity() {}

    public YearlySummaryEntity(Long userId, Integer year) {
        this.userId = userId;
        this.year = year;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String summary, String moodOverview, String keywordInsightsJson,
                       String highlightsJson, String letterToFuture, int recordCount, String sourceHash,
                       String model, String promptVersion) {
        this.title = title;
        this.summary = summary;
        this.moodOverview = moodOverview;
        this.keywordInsightsJson = keywordInsightsJson;
        this.highlightsJson = highlightsJson;
        this.letterToFuture = letterToFuture;
        this.recordCount = recordCount;
        this.sourceHash = sourceHash;
        this.model = model;
        this.promptVersion = promptVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Integer getYear() { return year; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getMoodOverview() { return moodOverview; }
    public String getKeywordInsightsJson() { return keywordInsightsJson; }
    public String getHighlightsJson() { return highlightsJson; }
    public String getLetterToFuture() { return letterToFuture; }
    public Integer getRecordCount() { return recordCount; }
    public String getSourceHash() { return sourceHash; }
    public String getModel() { return model; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
