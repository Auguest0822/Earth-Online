package com.earthonline.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 500)
        String avatar,

        @Size(max = 200)
        String slogan,

        LocalDate birthDate
) {
}