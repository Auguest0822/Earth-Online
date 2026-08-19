package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.KeywordInsightResponse;
import java.util.List;

public record DeepSeekYearlySummary(
        String title,
        String summary,
        String moodOverview,
        List<KeywordInsightResponse> keywordInsights,
        List<String> highlights,
        String letterToFuture
) {}
