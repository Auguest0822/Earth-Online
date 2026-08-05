package com.earthonline.backend.records.dto;

public record RecordResponse(
        Long id,
        String title,
        String content
) {
}