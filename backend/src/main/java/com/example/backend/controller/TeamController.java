package com.example.backend.controller;

import com.example.backend.dto.GameweekStatsResponse;
import com.example.backend.dto.PlayerSummary;
import com.example.backend.dto.SaveLineupRequest;
import com.example.backend.dto.SubstitutionRequest;
import com.example.backend.dto.TeamResponse;
import com.example.backend.dto.TeamLineupResponse;
import com.example.backend.dto.TransferRequest;
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

    @PostMapping("/save")
    @Operation(summary = "Save a completely new 15-player squad")
    public ResponseEntity<TeamResponse> saveTeam(@RequestBody List<Long> playerIds) {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.saveFullSquad(userId, playerIds);
        return ResponseEntity.ok(toResponse(team));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Make transfers on an existing squad (with point deduction)")
    public ResponseEntity<TeamResponse> saveTransfers(@RequestBody TransferRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.saveTransfers(userId, request.getPlayerIds(), request.getCost());
        return ResponseEntity.ok(toResponse(team));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get squad statistics (positions, budget, points)")
    public ResponseEntity<TeamManagementService.SquadStatistics> getStats() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getSquadStatistics(userId));
    }

    @GetMapping("/lineup")
    @Operation(summary = "Get your squad with starter and bench flags")
    public ResponseEntity<TeamLineupResponse> getLineup() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getTeamLineup(userId));
    }

    @PostMapping("/substitutions")
    @Operation(summary = "Swap a starter with a bench player")
    public ResponseEntity<TeamLineupResponse> makeSubstitution(@RequestBody SubstitutionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        TeamLineupResponse lineup = teamManagementService.makeSubstitution(
                userId,
                request.starterPlayerId(),
                request.benchPlayerId());
        return ResponseEntity.ok(lineup);
    }

    @PostMapping("/lineup/save")
    @Operation(summary = "Save starter and bench setup in one action")
    public ResponseEntity<Void> saveLineup(@RequestBody SaveLineupRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        teamManagementService.saveLineup(userId, request.starterPlayerIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/gameweek/{gameweek}")
    @Operation(summary = "Get gameweek specific squad score and global max points")
    public ResponseEntity<GameweekStatsResponse> getGameweekStats(@PathVariable int gameweek) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getGameweekStats(userId, gameweek));
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
