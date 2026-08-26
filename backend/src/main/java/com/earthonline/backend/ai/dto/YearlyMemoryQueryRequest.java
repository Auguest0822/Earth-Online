package com.earthonline.backend.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record YearlyMemoryQueryRequest(
        @Min(2000) @Max(2100) int year,
        @NotBlank(message = "请输入想问的问题")
        @Size(max = 200, message = "问题不能超过200个字符") String question
) {
}
