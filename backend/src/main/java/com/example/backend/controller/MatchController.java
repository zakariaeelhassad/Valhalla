package com.example.backend.controller;

import com.example.backend.dto.MatchEventDTO;
import com.example.backend.dto.MatchStatusResponse;
import com.example.backend.model.Match;
import com.example.backend.model.Gameweek;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.repository.MatchRepository;
import com.example.backend.service.MatchEventCacheService;
import com.example.backend.service.SimulatedClockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Gameweek fixtures with simulated live status and events")
public class MatchController {

        private final MatchRepository matchRepository;
        private final GameweekRepository gameweekRepository;
        private final SimulatedClockService clockService;
        private final MatchEventCacheService eventCache;

        @GetMapping("/gameweek/{gameweekNumber}")
        @Operation(summary = "All fixtures for a given gameweek with live status and events")
        public ResponseEntity<List<MatchStatusResponse>> getByGameweek(
                        @PathVariable Integer gameweekNumber) {

                Optional<Gameweek> gwOpt = gameweekRepository.findByGameweekNumber(gameweekNumber);
                if (gwOpt.isEmpty())
                        return ResponseEntity.notFound().build();

                List<Match> matches = matchRepository.findByGameweekId(gwOpt.get().getId());
                List<MatchStatusResponse> response = matches.stream()
                                .sorted(Comparator.comparing(Match::getKickoffTime))
                                .map(m -> toDto(m, gameweekNumber))
                                .collect(Collectors.toList());

                return ResponseEntity.ok(response);
        }

        @GetMapping("/live")
        @Operation(summary = "All currently LIVE matches across any gameweek")
        public ResponseEntity<List<MatchStatusResponse>> getLive() {
                List<MatchStatusResponse> live = matchRepository.findAll().stream()
                                .map(m -> toDto(m, m.getGameweek().getGameweekNumber()))
                                .filter(m -> "LIVE".equals(m.status()))
                                .sorted(Comparator.comparing(MatchStatusResponse::kickoffTime))
                                .collect(Collectors.toList());
                return ResponseEntity.ok(live);
        }

        @GetMapping("/gameweeks")
        @Operation(summary = "All gameweek numbers with match summary")
        public ResponseEntity<List<GameweekSummary>> getGameweeks() {
                List<Gameweek> gws = gameweekRepository.findAll();
                gws.sort(Comparator.comparingInt(Gameweek::getGameweekNumber));
                LocalDateTime now = clockService.getSimulatedNow();

                List<GameweekSummary> result = gws.stream().map(gw -> {
                        List<Match> matches = matchRepository.findByGameweekId(gw.getId());
                        long liveCount = matches.stream().filter(m -> "LIVE"
                                        .equals(clockService.computeStatus(m.getKickoffTime(), m.getFinished())))
                                        .count();
                        long finishedCount = matches.stream().filter(m -> "FINISHED"
                                        .equals(clockService.computeStatus(m.getKickoffTime(), m.getFinished())))
                                        .count();
                        String gwStatus = liveCount > 0 ? "LIVE"
                                        : finishedCount == matches.size() ? "COMPLETED"
                                                        : gw.getStartDate().isAfter(now) ? "UPCOMING" : "ACTIVE";
                        return new GameweekSummary(gw.getGameweekNumber(), matches.size(), (int) liveCount,
                                        (int) finishedCount, gwStatus);
                }).collect(Collectors.toList());

                return ResponseEntity.ok(result);
        }

        @GetMapping("/clock")
        @Operation(summary = "Current simulated time")
        public ResponseEntity<Map<String, String>> getClock() {
                return ResponseEntity.ok(Map.of("simulatedNow", clockService.getSimulatedNow().toString()));
        }

        private MatchStatusResponse toDto(Match m, int gwNumber) {
                String status = clockService.computeStatus(m.getKickoffTime(), m.getFinished());
                int elapsed = "LIVE".equals(status) ? clockService.getElapsedMinutes(m.getKickoffTime()) : 0;
                List<MatchEventDTO> events = eventCache.getEvents(m.getHomeTeam(), m.getAwayTeam(), gwNumber);
                return new MatchStatusResponse(
                                m.getId(), gwNumber,
                                m.getHomeTeam(), m.getAwayTeam(),
                                m.getHomeScore(), m.getAwayScore(),
                                m.getKickoffTime(), m.getFinished(),
                                status, elapsed, events);
        }

        public record GameweekSummary(int gameweekNumber, int totalMatches, int liveMatches, int finishedMatches,
                        String status) {
        }
}
