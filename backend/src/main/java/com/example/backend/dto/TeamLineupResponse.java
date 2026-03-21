package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeamLineupResponse(
        Long teamId,
        String teamName,
        BigDecimal remainingBudget,
        List<TeamLineupPlayerResponse> players) {
}
