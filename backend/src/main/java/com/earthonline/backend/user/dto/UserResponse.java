package com.earthonline.backend.user.dto;

public record UserResponse(
        Long id,
        String username,
        String email
) {
}
