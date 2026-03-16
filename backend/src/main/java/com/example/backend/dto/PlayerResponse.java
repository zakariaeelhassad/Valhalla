package com.example.backend.dto;

import com.example.backend.model.Position;

import java.math.BigDecimal;

public record PlayerResponse(
        Long id,
        String name,
        Position position,
        String realTeam,
        BigDecimal price,
        Integer totalPoints,
        Integer goalPoints) {
}
