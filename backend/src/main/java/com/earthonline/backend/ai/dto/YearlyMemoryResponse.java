package com.earthonline.backend.ai.dto;

import java.util.List;

public record YearlyMemoryResponse(
        String question,
        String answer,
        int year,
        List<MemorySourceResponse> sources
) {
}
