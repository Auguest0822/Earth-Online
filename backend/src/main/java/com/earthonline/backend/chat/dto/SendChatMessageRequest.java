package com.earthonline.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*

public record SendChatMessageRequest(
        @NotBlank(message = "接收方地球 ID 不能为空")
        @Pattern(regexp = "\\d{10}", message = "接收方地球 ID 必须为 10 位数字")
        String recipientPublicId,
        @NotBlank(message = "消息不能为空")
        @Size(max = 500, message = "消息不能超过 500 个字符")
        String content
) {
}
*/

public record SendChatMessageRequest(
        @NotBlank @Pattern(regexp = "\\d{10}") String recipientPublicId,
        @NotBlank @Size(max = 500) String content,
        @Size(max = 64) String clientMessageId
) {
}
