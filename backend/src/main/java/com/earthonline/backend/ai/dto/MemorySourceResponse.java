package com.earthonline.backend.ai.dto;

import java.time.LocalDate;

public record MemorySourceResponse(
        Long recordId,
        String title,
        LocalDate date,
        String excerpt,
        double score
) {
}
