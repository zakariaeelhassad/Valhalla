package com.example.backend.dto;

public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String teamName,
        Integer totalPoints) {
}
