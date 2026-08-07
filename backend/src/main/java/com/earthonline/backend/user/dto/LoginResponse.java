package com.earthonline.backend.user.dto;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {
}