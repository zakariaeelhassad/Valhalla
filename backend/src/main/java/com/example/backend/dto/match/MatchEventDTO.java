package com.example.backend.dto.match;

public record MatchEventDTO(
        String type,
        String player,
        String team,
        int minute) {
}
