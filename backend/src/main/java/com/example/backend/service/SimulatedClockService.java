package com.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SimulatedClockService {

    // Configuration

    private static final LocalDateTime SEASON_START = LocalDateTime.of(2024, 8, 16, 18, 0, 0); // 1h before first KO

    private static final long SPEED_MULTIPLIER = 60L;

    // State

    private final long anchorEpochSecond;

    public SimulatedClockService() {
        this.anchorEpochSecond = System.currentTimeMillis() / 1000L;
    }

    // Public API

    public LocalDateTime getSimulatedNow() {
        long realSecondsElapsed = (System.currentTimeMillis() / 1000L) - anchorEpochSecond;
        long simSecondsElapsed = realSecondsElapsed * SPEED_MULTIPLIER;
        return SEASON_START.plusSeconds(simSecondsElapsed);
    }

    public String computeStatus(LocalDateTime kickoffTime, boolean finished) {
        LocalDateTime now = getSimulatedNow();

        if (finished && kickoffTime.isBefore(now)) {
            return "FINISHED";
        }
        if (kickoffTime.isAfter(now)) {
            return "SCHEDULED";
        }
        // Between kickoff and kickoff+105 min (90 regular + 15 buffer) = LIVE
        LocalDateTime matchEnd = kickoffTime.plusMinutes(105);
        if (now.isBefore(matchEnd)) {
            return "LIVE";
        }
        return "FINISHED";
    }

    public int getElapsedMinutes(LocalDateTime kickoffTime) {
        LocalDateTime now = getSimulatedNow();
        if (kickoffTime.isAfter(now))
            return 0;
        long secs = java.time.Duration.between(kickoffTime, now).getSeconds();
        return (int) Math.min(secs / 60, 90);
    }
}
