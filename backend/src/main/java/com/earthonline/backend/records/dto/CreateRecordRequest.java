package com.earthonline.backend.records.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateRecordRequest(
        @Size(max = 100, message = "标题不能超过 100 个字符")
        String title,

        @NotBlank(message = "内容不能为空")
        @Size(max = 1500, message = "事记内容不能超过 1500 字")
        String content,

        LocalDate date,

        @Size(max = 32, message = "事件类型不能超过 32 个字符")
        String eventType,

        @Size(max = 32, message = "心情格式不正确")
        String mood,

        @Size(max = 5, message = "最多 5 个关键词")
        List<@Size(max = 30, message = "关键词不能超过 30 个字符") String> keywords,

        @Size(max = 9, message = "每条记录最多 9 张图片")
        List<@Size(max = 1000, message = "图片地址过长") String> photos
) {
    public CreateRecordRequest {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        photos = photos == null ? List.of() : List.copyOf(photos);
    }
}
