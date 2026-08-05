package com.earthonline.backend.records.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecordRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题不能超过100个字符")
        String title,

        @NotBlank(message = "内容不能为空")
        @Size(max = 5000, message = "内容不能超过5000个字符")
        String content
) {
}