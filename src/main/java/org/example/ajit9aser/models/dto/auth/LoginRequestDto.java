package org.example.ajit9aser.models.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank
        String email,

        @NotBlank
        String password
) {}
