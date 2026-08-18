package com.earthonline.backend.records.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecordResponse(
        Long id,
        String title,
        String content,
        LocalDate date,
        String eventType,
        String mood,
        List<String> keywords,
        List<String> photos,
        List<String> photoObjectKeys,
        LocalDateTime createdAt
) {
    public List<String> moods() {
        return mood == null || mood.isBlank() ? List.of() : List.of(mood);
    }
}
