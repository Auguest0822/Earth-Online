package com.earthonline.backend.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record YearlySummaryRequest(
        @Min(1900) @Max(2100) int year,
        boolean regenerate
) {}
