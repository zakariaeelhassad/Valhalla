package com.example.backend.service;

import com.example.backend.service.DataInitializationService.*;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.repository.MatchRepository;
import com.example.backend.model.entity.Match;
import com.example.backend.model.entity.Gameweek;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GameEngineService {

    private final ScoringService scoringService;
    private final CommentaryService commentaryService;
    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;
    private final GameweekRepository gameweekRepository;
    private final SimulatedClockService clockService;

    // Game state
    private List<MatchJsonDTO> allMatches;
    private AtomicInteger currentMatchIndex = new AtomicInteger(0);
    private AtomicInteger currentMinute = new AtomicInteger(0);
    private AtomicInteger currentGameweek = new AtomicInteger(1);
    private volatile boolean engineRunning = false;
    private volatile boolean matchInProgress = false;

    @PostConstruct
    public void initialize() {
        try {
            loadMatchData();
            log.info("Game Engine initialized with {} matches", allMatches.size());
        } catch (IOException e) {
            log.error("Failed to initialize Game Engine", e);
        }
    }

    private void loadMatchData() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/fique_data");
        allMatches = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<MatchJsonDTO>>() {
                });
    }

    @Scheduled(fixedRate = 5000)
    public void simulateMatchMinute() {
        if (!engineRunning || allMatches == null || allMatches.isEmpty()) {
            return;
        }

        try {
            if (!matchInProgress) {
                startNextMatch();
                return;
            }

            int minute = currentMinute.incrementAndGet();
            int matchIdx = currentMatchIndex.get();

            if (matchIdx >= allMatches.size()) {
                log.info("All matches completed!");
                engineRunning = false;
                return;
            }

            MatchJsonDTO currentMatch = allMatches.get(matchIdx);

            log.info("⚽ Minute {}: {} vs {} (Gameweek {})",
                    minute,
                    currentMatch.getHomeTeam(),
                    currentMatch.getAwayTeam(),
                    currentMatch.getGameweek());

            // Check if match is complete
            if (minute >= 90) {
                completeMatch(currentMatch);
            }

        } catch (Exception e) {
            log.error("Error in game engine simulation", e);
        }
    }

    private void startNextMatch() {
        int matchIdx = currentMatchIndex.get();

        if (matchIdx >= allMatches.size()) {
            log.info("No more matches to simulate");
            engineRunning = false;
            return;
        }

        MatchJsonDTO match = allMatches.get(matchIdx);
        currentMinute.set(0);
        matchInProgress = true;
        currentGameweek.set(match.getGameweek());

        log.info("🎮 Starting Match {}: {} vs {} (Gameweek {})",
                matchIdx + 1,
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getGameweek());
    }

    private void processMatchEvents(MatchJsonDTO match) {
        if (match.getEvents() == null) {
            return;
        }

        int gameweek = match.getGameweek();
        EventsJsonDTO events = match.getEvents();

        // Process Goals
        if (events.getGoals() != null) {
            for (EventPlayerJsonDTO goal : events.getGoals()) {
                log.info("⚽ GOAL! {} ({})", goal.getPlayer(), goal.getTeam());

                // Update scoring
                scoringService.processGoal(goal.getPlayer(), goal.getTeam(), gameweek);

                // Generate commentary
                String commentary = commentaryService.generateCommentary(
                        goal.getPlayer(),
                        "GOAL",
                        currentMinute.get());
                log.info("📢 Commentary: {}", commentary);
            }
        }

        // Process Assists
        if (events.getAssists() != null) {
            for (EventPlayerJsonDTO assist : events.getAssists()) {
                log.info("🎯 ASSIST! {} ({})", assist.getPlayer(), assist.getTeam());
                scoringService.processAssist(assist.getPlayer(), assist.getTeam(), gameweek);
            }
        }

        // Process Yellow Cards
        if (events.getYellowCards() != null) {
            for (EventPlayerJsonDTO card : events.getYellowCards()) {
                log.info("🟨 Yellow Card: {} ({})", card.getPlayer(), card.getTeam());
                scoringService.processYellowCard(card.getPlayer(), card.getTeam(), gameweek);
            }
        }

        // Process Red Cards
        if (events.getRedCards() != null) {
            for (EventPlayerJsonDTO card : events.getRedCards()) {
                log.info("🟥 RED CARD! {} ({})", card.getPlayer(), card.getTeam());

                // Update scoring
                scoringService.processRedCard(card.getPlayer(), card.getTeam(), gameweek);

                // Generate commentary for red cards
                String commentary = commentaryService.generateCommentary(
                        card.getPlayer(),
                        "RED_CARD",
                        currentMinute.get());
                log.info("📢 Commentary: {}", commentary);
            }
        }
    }

    private void completeMatch(MatchJsonDTO match) {
        log.info("⏱️ Full Time: {} {} - {} {}",
                match.getHomeTeam(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getAwayTeam());

        // Apply all scoring only after full-time.
        processMatchEvents(match);

        // Process clean sheets
        if (match.getAwayScore() == 0) {
            log.info("🛡️ Clean Sheet for {}", match.getHomeTeam());
            scoringService.processCleanSheet(match.getHomeTeam(), match.getGameweek());
        }
        if (match.getHomeScore() == 0) {
            log.info("🛡️ Clean Sheet for {}", match.getAwayTeam());
            scoringService.processCleanSheet(match.getAwayTeam(), match.getGameweek());
        }

        // Process Minutes Played (Appearance Points)
        if (match.getLineups() != null) {
            log.info("📊 Processing minutes played for {}", match.getHomeTeam());
            processLineupMinutes(match.getLineups().getHome(), match.getHomeTeam(), match.getGameweek());

            log.info("📊 Processing minutes played for {}", match.getAwayTeam());
            processLineupMinutes(match.getLineups().getAway(), match.getAwayTeam(), match.getGameweek());
        }

        // Move to next match
        currentMatchIndex.incrementAndGet();
        matchInProgress = false;
        currentMinute.set(0);

        log.info("✅ Match completed. Moving to next match...\n");
    }

    private void processLineupMinutes(LineupJsonDTO lineup, String team, int gameweek) {
        if (lineup == null)
            return;

        processPlayerMinutes(lineup.getStarting(), team, gameweek);
        processPlayerMinutes(lineup.getSubstitutesIn(), team, gameweek);
        processPlayerMinutes(lineup.getBenchUnused(), team, gameweek);
    }

    private void processPlayerMinutes(List<PlayerJsonDTO> players, String team, int gameweek) {
        if (players == null)
            return;
        for (PlayerJsonDTO p : players) {
            if (p.getMinutes() != null && p.getMinutes() > 0) {
                scoringService.processMinutesPlayed(p.getName(), team, gameweek, p.getMinutes());
            }
        }
    }

    // Control methods
    public void startEngine() {
        if (!engineRunning) {
            engineRunning = true;
            matchInProgress = false;
            log.info("🎮 Game Engine STARTED");
        }
    }

    public void stopEngine() {
        engineRunning = false;
        matchInProgress = false;
        log.info("⏸️ Game Engine STOPPED");
    }

    public void resetEngine() {
        currentMatchIndex.set(0);
        currentMinute.set(0);
        currentGameweek.set(1);
        matchInProgress = false;
        engineRunning = false;
        log.info("🔄 Game Engine RESET");
    }

    public GameState getGameState() {
        boolean isGameweekActive = calculateGameweekActive(currentGameweek.get());

        if (allMatches == null || currentMatchIndex.get() >= allMatches.size()) {
            return new GameState(
                    engineRunning,
                    matchInProgress,
                    isGameweekActive,
                    currentGameweek.get(),
                    currentMatchIndex.get(),
                    currentMinute.get(),
                    null,
                    null,
                    0,
                    0);
        }

        MatchJsonDTO currentMatch = allMatches.get(currentMatchIndex.get());
        return new GameState(
                engineRunning,
                matchInProgress,
                isGameweekActive,
                currentGameweek.get(),
                currentMatchIndex.get(),
                currentMinute.get(),
                currentMatch.getHomeTeam(),
                currentMatch.getAwayTeam(),
                currentMatch.getHomeScore(),
                currentMatch.getAwayScore());
    }

    /**
     * Helper to compute true active status from DB matches
     */
    private boolean calculateGameweekActive(int gameweekNum) {
        Optional<Gameweek> gwOpt = gameweekRepository.findByGameweekNumber(gameweekNum);
        if (gwOpt.isEmpty()) {
            return false;
        }

        List<Match> matches = matchRepository.findByGameweekId(gwOpt.get().getId());
        if (matches.isEmpty()) {
            return false;
        }

        // If any match is LIVE or SCHEDULED, the gameweek is considered "active"
        // (transfers locked)
        // Only when all matches are FINISHED does it become false (transfers open)
        for (Match m : matches) {
            String status = clockService.computeStatus(m.getKickoffTime(), m.getFinished());
            if ("LIVE".equals(status) || "SCHEDULED".equals(status)) {
                return true;
            }
        }

        // Return false to indicate the round is totally complete and window is open
        return false;
    }

    /**
     * Convenience check used by TeamManagementService to enforce the transfer
     * window.
     */
    public boolean isGameweekActive() {
        return calculateGameweekActive(currentGameweek.get());
    }

    public record GameState(
            boolean engineRunning,
            boolean matchInProgress,
            boolean gameweekActive,
            int currentGameweek,
            int currentMatchIndex,
            int currentMinute,
            String homeTeam,
            String awayTeam,
            int homeScore,
            int awayScore) {
    }
}
