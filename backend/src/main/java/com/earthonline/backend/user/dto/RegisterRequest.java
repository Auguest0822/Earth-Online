package com.earthonline.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Size(max = 255)
        String email
) {
}
