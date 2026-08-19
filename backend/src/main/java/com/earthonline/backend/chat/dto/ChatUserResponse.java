package com.earthonline.backend.chat.dto;

public record ChatUserResponse(
        String publicId,
        String username,
        String avatar
) {
}
