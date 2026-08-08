package com.earthonline.backend.user.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String username,
        String email,
        String avatar,
        String slogan,
        LocalDate birthDate
) {
}