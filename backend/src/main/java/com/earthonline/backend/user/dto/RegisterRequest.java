package com.earthonline.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N}_]+$")
        String username,

        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$")
        String password,

        @NotBlank
        @Email
        @Size(max = 20)
        String email
) {
}
