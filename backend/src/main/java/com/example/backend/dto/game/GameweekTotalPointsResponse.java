package com.example.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameweekTotalPointsResponse {
    private int gameweek;
    private int points;
}
