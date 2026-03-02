package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating a fantasy team
 * Business Rules:
 * - Exactly 15 players required
 * - Total cost must not exceed 100.0
 */
public record TeamCreateRequest(
        @NotBlank(message = "Team name is required") @Size(max = 100, message = "Team name must not exceed 100 characters") String teamName,

        @NotNull(message = "Player IDs are required") @Size(min = 15, max = 15, message = "Exactly 15 players must be selected") List<Long> playerIds) {
}
