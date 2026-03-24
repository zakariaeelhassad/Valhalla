package com.example.backend.dto.game;

public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String teamName,
        Integer totalPoints) {
}
