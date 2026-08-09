package com.earthonline.backend.news.dto;

import java.time.OffsetDateTime;

public record NewsItemResponse(
        String id,
        String title,
        String summary,
        String source,
        String url,
        String image,
        String category,
        String region,
        OffsetDateTime publishedAt
) {
}
