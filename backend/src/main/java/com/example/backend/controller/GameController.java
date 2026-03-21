package com.example.backend.controller;

import com.example.backend.dto.LeaderboardEntryResponse;
import com.example.backend.model.UserTeam;
import com.example.backend.repository.UserTeamRepository;
import com.example.backend.service.GameEngineService;
import com.example.backend.service.TransferWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Tag(name = "Game Engine", description = "Control the live match simulation engine and view leaderboard")
public class GameController {

    private final GameEngineService gameEngineService;
    private final TransferWindowService transferWindowService;
    private final UserTeamRepository userTeamRepository;

    @GetMapping("/state")
    @Operation(summary = "Get current game engine state")
    public ResponseEntity<GameEngineService.GameState> getState() {
        return ResponseEntity.ok(gameEngineService.getGameState());
    }

    @GetMapping("/transfer-window")
    @Operation(summary = "Get transfer window status based on backend date and DB gameweek dates")
    public ResponseEntity<TransferWindowService.TransferWindowStatus> getTransferWindowStatus() {
        return ResponseEntity.ok(transferWindowService.getTransferWindowStatus());
    }

    @PostMapping("/start")
    @Operation(summary = "Start the match simulation engine")
    public ResponseEntity<Map<String, String>> start() {
        gameEngineService.startEngine();
        return ResponseEntity.ok(Map.of("status", "Game engine started"));
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop the match simulation engine")
    public ResponseEntity<Map<String, String>> stop() {
        gameEngineService.stopEngine();
        return ResponseEntity.ok(Map.of("status", "Game engine stopped"));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset the engine back to match 1, gameweek 1")
    public ResponseEntity<Map<String, String>> reset() {
        gameEngineService.resetEngine();
        return ResponseEntity.ok(Map.of("status", "Game engine reset"));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get the top 20 fantasy teams ranked by total points")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard() {
        List<UserTeam> allTeams = userTeamRepository.findAll();

        allTeams.sort(Comparator.comparingInt(UserTeam::getTotalPoints).reversed());

        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < Math.min(allTeams.size(), 20); i++) {
            UserTeam team = allTeams.get(i);
            leaderboard.add(new LeaderboardEntryResponse(
                    i + 1,
                    team.getUser().getId(),
                    team.getUser().getUsername(),
                    team.getTeamName(),
                    team.getTotalPoints()));
        }

        return ResponseEntity.ok(leaderboard);
    }
}
