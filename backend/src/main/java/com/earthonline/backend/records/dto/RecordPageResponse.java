package com.earthonline.backend.records.dto;

import java.util.List;

public record RecordPageResponse(
        List<RecordResponse> items,
        long total,
        int page,
        int pageSize,
        int totalPages,
        List<Integer> years,
        List<String> tags,
        List<String> eventTypes
) {
}
