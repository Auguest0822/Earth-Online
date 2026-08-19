package com.earthonline.backend.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record YearlySummaryResponse(
        Long id,
        int year,
        String title,
        String summary,
        String moodOverview,
        List<KeywordInsightResponse> keywordInsights,
        List<String> highlights,
        String letterToFuture,
        int recordCount,
        String model,
        boolean stale,
        LocalDateTime updatedAt
) {}
