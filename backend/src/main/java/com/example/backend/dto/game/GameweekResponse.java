package com.example.backend.dto.game;

import java.time.LocalDateTime;

public record GameweekResponse(
        Long id,
        Integer gameweekNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status) {
}
