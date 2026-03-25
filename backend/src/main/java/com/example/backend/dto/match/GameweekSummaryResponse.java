package com.example.backend.dto.match;

public record GameweekSummaryResponse(
        int gameweekNumber,
        int totalMatches,
        int liveMatches,
        int finishedMatches,
        String status) {
}