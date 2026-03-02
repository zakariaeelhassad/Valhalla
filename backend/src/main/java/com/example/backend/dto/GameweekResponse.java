package com.example.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for gameweek information
 */
public record GameweekResponse(
        Long id,
        Integer gameweekNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status) {
}
