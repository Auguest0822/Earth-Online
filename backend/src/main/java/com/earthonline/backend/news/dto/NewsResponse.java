package com.earthonline.backend.news.dto;

import java.util.List;

public record NewsResponse(
        List<NewsItemResponse> events,
        NewsItemResponse featured,
        List<Object> categoryStats,
        List<Object> regionStats,
        List<Object> trends,
        NewsMetaResponse meta
) {
}
