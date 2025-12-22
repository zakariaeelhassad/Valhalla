package org.example.ajit9aser.models.dto.user;

import java.time.LocalDateTime;

public record UserResponseDto(

        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt
) {}
