package com.example.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameweekTransferCountResponse {
    private int gameweek;
    private int transferCount;
}
