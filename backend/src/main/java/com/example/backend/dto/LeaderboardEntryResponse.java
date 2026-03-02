package com.example.backend.dto;

/**
 * DTO for a single leaderboard entry
 */
public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String teamName,
        Integer totalPoints) {
}
