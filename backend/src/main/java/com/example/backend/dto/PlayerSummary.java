package com.example.backend.dto;

import com.example.backend.model.Position;

import java.math.BigDecimal;

public record PlayerSummary(
        Long id,
        String name,
        Position position,
        String realTeam,
        BigDecimal price,
        Integer totalPoints) {
}
