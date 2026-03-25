package com.example.backend.dto.match;

import java.util.List;

public record CurrentGameweekResponse(
        String currentDate,
        Integer currentGameweek,
        List<MatchStatusResponse> matches) {
}