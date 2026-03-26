package com.example.backend.controller;

import com.example.backend.dto.game.GameweekStatsResponse;
import com.example.backend.dto.game.GameweekTotalPointsResponse;
import com.example.backend.dto.game.GameweekTransferCountResponse;
import com.example.backend.dto.team.SaveLineupRequest;
import com.example.backend.dto.team.SquadStatisticsResponse;
import com.example.backend.dto.team.SubstitutionRequest;
import com.example.backend.dto.team.TeamResponse;
import com.example.backend.dto.team.TeamLineupResponse;
import com.example.backend.dto.team.TransferRequest;
import com.example.backend.mapper.TeamMapper;
import com.example.backend.model.entity.UserTeam;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.TeamManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "Team", description = "Manage your fantasy squad")
public class TeamController {

    private final TeamManagementService teamManagementService;
    private final SecurityUtils securityUtils;
    private final TeamMapper teamMapper;

    @GetMapping
    @Operation(summary = "Get your current squad")
    public ResponseEntity<TeamResponse> getMyTeam() {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @PostMapping("/players/{playerId}")
    @Operation(summary = "Add a player to your squad")
    public ResponseEntity<TeamResponse> addPlayer(@PathVariable Long playerId) {
        Long userId = securityUtils.getCurrentUserId();
        teamManagementService.addPlayerToSquad(userId, playerId);
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @DeleteMapping("/players/{playerId}")
    @Operation(summary = "Remove a player from your squad")
    public ResponseEntity<TeamResponse> removePlayer(@PathVariable Long playerId) {
        Long userId = securityUtils.getCurrentUserId();
        teamManagementService.removePlayerFromSquad(userId, playerId);
        UserTeam team = teamManagementService.getUserSquad(userId);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @PostMapping("/save")
    @Operation(summary = "Save a completely new 15-player squad")
    public ResponseEntity<TeamResponse> saveTeam(@RequestBody List<Long> playerIds) {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.saveFullSquad(userId, playerIds);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Make transfers on an existing squad (with point deduction)")
    public ResponseEntity<TeamResponse> saveTransfers(@RequestBody TransferRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        UserTeam team = teamManagementService.saveTransfers(userId, request.getPlayerIds(), request.getCost());
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get squad statistics (positions, budget, points)")
    public ResponseEntity<SquadStatisticsResponse> getStats() {
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

    @GetMapping("/gameweek/transfers/count")
    @Operation(summary = "Get transfer count for current gameweek")
    public ResponseEntity<GameweekTransferCountResponse> getCurrentGameweekTransferCount() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getCurrentGameweekTransferCount(userId));
    }

    @GetMapping("/gameweek-points")
    @Operation(summary = "Get persisted total points per gameweek for your team")
    public ResponseEntity<List<GameweekTotalPointsResponse>> getGameweekPoints() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(teamManagementService.getPersistedGameweekTotals(userId));
    }

}

