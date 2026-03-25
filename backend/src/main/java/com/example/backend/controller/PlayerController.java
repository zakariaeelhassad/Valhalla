package com.example.backend.controller;

import com.example.backend.dto.player.PlayerResponse;
import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.model.enums.Position;
import com.example.backend.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Browse and search fantasy-eligible players")
public class PlayerController {

        private final PlayerService playerService;

    @GetMapping
    @Operation(summary = "Get all players (paginated)", description = "Filter by position and/or team. Sort by price, totalPoints, or name.")
    public ResponseEntity<Page<PlayerSummary>> getPlayers(
            @RequestParam(required = false) Position position,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalPoints") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(playerService.getPlayers(position, team, search, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single player by ID")
    public ResponseEntity<PlayerResponse> getPlayer(@PathVariable Long id) {
                return ResponseEntity.ok(playerService.getPlayer(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search players by name")
    public ResponseEntity<List<PlayerSummary>> searchPlayers(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(playerService.searchPlayers(q, limit));
    }

    @GetMapping("/teams")
    @Operation(summary = "Get all distinct real-world team names")
    public ResponseEntity<List<String>> getTeams() {
                return ResponseEntity.ok(playerService.getTeams());
    }
}
