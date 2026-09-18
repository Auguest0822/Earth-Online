package com.earthonline.backend.ai;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * “问问这一年”的问答缓存表（一个用户一年只保留一条）。
 *
 * 用户每次提问都会覆盖这一行，所以再次进入年度记录页时看到的就是“上一次的问答内容”，
 * 不需要重新调用 AI（省时间、也省 token）。做法与年度总结缓存（yearly_summaries）完全一致。
 */
@Entity
@Table(name = "yearly_memory_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_yearly_memory_user_year", columnNames = {"user_id", "summary_year"})
})
public class YearlyMemoryAnswerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "summary_year", nullable = false) private Integer year;
    @Column(nullable = false, length = 500) private String question;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String answer;
    /** 回答依据（引用到的记录）以 JSON 形式整段保存，读出来即可原样还原“回答依据”卡片。 */
    @Lob @Column(name = "sources_json", nullable = false, columnDefinition = "TEXT") private String sourcesJson;
    /** 缓存时这一年记录的内容指纹，用来判断缓存是否过期。 */
    @Column(name = "source_hash", nullable = false, length = 64) private String sourceHash;
    @Column(nullable = false, length = 64) private String model;
    @Column(name = "prompt_version", nullable = false, length = 32) private String promptVersion;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected YearlyMemoryAnswerEntity() {}

    public YearlyMemoryAnswerEntity(Long userId, Integer year) {
        this.userId = userId;
        this.year = year;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String question, String answer, String sourcesJson, String sourceHash,
                       String model, String promptVersion) {
        this.question = question;
        this.answer = answer;
        this.sourcesJson = sourcesJson;
        this.sourceHash = sourceHash;
        this.model = model;
        this.promptVersion = promptVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Integer getYear() { return year; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getSourcesJson() { return sourcesJson; }
    public String getSourceHash() { return sourceHash; }
    public String getModel() { return model; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
