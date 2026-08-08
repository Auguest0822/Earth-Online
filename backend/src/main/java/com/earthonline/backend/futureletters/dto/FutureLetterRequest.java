package com.earthonline.backend.futureletters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FutureLetterRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String content,

        @Size(max = 50)
        String mood,

        @NotNull
        LocalDate openAt,

        @Size(max = 30)
        String status,

        @Size(max = 30)
        String visibility,

        Boolean allowEditBeforeOpen
) {
}