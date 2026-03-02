package com.example.backend.controller;

import com.example.backend.dto.PlayerSummary;
import com.example.backend.dto.TeamResponse;
import com.example.backend.model.UserTeam;
import com.example.backend.model.UserTeamPlayer;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.TeamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Manage your fantasy squad")
public class TeamController {

    private final TeamManagementService teamManagementService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get your current squad")
    public ResponseEntity<TeamResponse> getMyTeam() {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(toResponse(team));
    }

    @PostMapping("/players/{playerId}")
    @Operation(summary = "Add a player to your squad")
    public ResponseEntity<TeamResponse> addPlayer(@PathVariable Long playerId) {
        Long userId = securityUtils.getCurrentUserId();
        teamManagementService.addPlayerToSquad(userId, playerId);
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(toResponse(team));
    }

    @DeleteMapping("/players/{playerId}")
    @Operation(summary = "Remove a player from your squad")
    public ResponseEntity<TeamResponse> removePlayer(@PathVariable Long playerId) {
        Long userId = securityUtils.getCurrentUserId();
        teamManagementService.removePlayerFromSquad(userId, playerId);
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(toResponse(team));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get squad statistics (positions, budget, points)")
    public ResponseEntity<TeamManagementService.SquadStatistics> getStats() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getSquadStatistics(userId));
    }

    private TeamResponse toResponse(UserTeam team) {
        List<PlayerSummary> players = team.getTeamPlayers().stream()
                .map(UserTeamPlayer::getPlayer)
                .map(p -> new PlayerSummary(p.getId(), p.getName(), p.getPosition(),
                        p.getRealTeam(), p.getPrice(), p.getTotalPoints()))
                .collect(Collectors.toList());

        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getBudget(),
                team.getRemainingBudget(),
                team.getTotalPoints(),
                players,
                players.size());
    }
}
