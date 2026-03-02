package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for match information including simulated live status and timed events.
 */
public record MatchStatusResponse(
                Long id,
                Integer gameweekNumber,
                String homeTeam,
                String awayTeam,
                Integer homeScore,
                Integer awayScore,
                LocalDateTime kickoffTime,
                Boolean finished,
                /** SCHEDULED | LIVE | FINISHED */
                String status,
                /** Elapsed minutes if LIVE (0 otherwise) */
                int elapsedMinutes,
                /** All events for this match, each with an assigned simulated minute */
                List<MatchEventDTO> events) {
}
