package com.earthonline.backend.friends.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateFriendRequest(
        @NotBlank(message = "地球 ID 不能为空")
        @Pattern(regexp = "\\d{10}", message = "地球 ID 必须是 10 位数字")
        String publicId
) {
}
