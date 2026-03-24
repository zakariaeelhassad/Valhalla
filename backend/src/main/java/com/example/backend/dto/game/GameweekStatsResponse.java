package com.example.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameweekStatsResponse {
    private int gameweek;
    private int teamPoints;
    private int globalHighestPoints;
    private List<PlayerGameweekScore> players;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerGameweekScore {
        private Long playerId;
        private String name;
        private String position;
        private String realTeam;
        private double price;
        private int totalPoints;
        private int points;
        private boolean starter;
    }
}
