package com.example.backend.dto.match;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Integer gameweekNumber,
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        LocalDateTime kickoffTime,
        Boolean finished) {
}
