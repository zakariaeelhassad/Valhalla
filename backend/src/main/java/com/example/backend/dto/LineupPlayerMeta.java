package com.example.backend.dto;

import com.example.backend.model.Position;

public record LineupPlayerMeta(
        Long playerId,
        Position position) {
}
