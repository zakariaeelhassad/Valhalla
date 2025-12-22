package org.example.ajit9aser.models.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequestDto(

        @NotBlank
        String username,

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password
) {}