package com.earthonline.backend.news.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record NewsMetaResponse(
        String sourceMode,
        List<String> sources,
        OffsetDateTime updatedAt,
        boolean cached,
        String dataStatus,
        String message
) {
}
