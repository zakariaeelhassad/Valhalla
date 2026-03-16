package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchStatusResponse(
                Long id,
                Integer gameweekNumber,
                String homeTeam,
                String awayTeam,
                Integer homeScore,
                Integer awayScore,
                LocalDateTime kickoffTime,
                Boolean finished,
                String status,
                int elapsedMinutes,
                List<MatchEventDTO> events) {
}
