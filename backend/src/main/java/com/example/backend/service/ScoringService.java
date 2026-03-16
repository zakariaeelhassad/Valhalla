package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repository.PlayerGameweekStatsRepository;
import com.example.backend.repository.PlayerRepository;
import com.example.backend.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final PlayerRepository playerRepository;
    private final PlayerGameweekStatsRepository statsRepository;
    private final UserTeamRepository userTeamRepository;

    // Point values
    private static final int ASSIST_POINTS = 3;
    private static final int YELLOW_CARD_POINTS = -1;
    private static final int RED_CARD_POINTS = -3;
    private static final int CLEAN_SHEET_GK_DEF_POINTS = 4;
    private static final int CLEAN_SHEET_MID_POINTS = 1;

    @Transactional
    public void processGoal(String playerName, String team, int gameweek) {
        log.info("Processing goal for player: {} ({}), gameweek: {}", playerName, team, gameweek);

        Optional<Player> playerOpt = findPlayerByNameAndTeam(playerName, team);
        if (playerOpt.isEmpty()) {
            log.warn("Player not found: {} ({})", playerName, team);
            return;
        }

        Player player = playerOpt.get();
        int points = player.getGoalPoints(); // Uses position-based calculation from Player entity

        updatePlayerStats(player, gameweek, EventType.GOAL, points);
        updateAllUserTeamPoints();

        log.info("Goal processed: {} earned {} points", playerName, points);
    }

    @Transactional
    public void processAssist(String playerName, String team, int gameweek) {
        log.info("Processing assist for player: {} ({}), gameweek: {}", playerName, team, gameweek);

        Optional<Player> playerOpt = findPlayerByNameAndTeam(playerName, team);
        if (playerOpt.isEmpty()) {
            log.warn("Player not found: {} ({})", playerName, team);
            return;
        }

        Player player = playerOpt.get();
        updatePlayerStats(player, gameweek, EventType.ASSIST, ASSIST_POINTS);
        updateAllUserTeamPoints();

        log.info("Assist processed: {} earned {} points", playerName, ASSIST_POINTS);
    }

    @Transactional
    public void processYellowCard(String playerName, String team, int gameweek) {
        log.info("Processing yellow card for player: {} ({}), gameweek: {}", playerName, team, gameweek);

        Optional<Player> playerOpt = findPlayerByNameAndTeam(playerName, team);
        if (playerOpt.isEmpty()) {
            log.warn("Player not found: {} ({})", playerName, team);
            return;
        }

        Player player = playerOpt.get();
        updatePlayerStats(player, gameweek, EventType.YELLOW_CARD, YELLOW_CARD_POINTS);
        updateAllUserTeamPoints();

        log.info("Yellow card processed: {} lost {} points", playerName, Math.abs(YELLOW_CARD_POINTS));
    }

    @Transactional
    public void processRedCard(String playerName, String team, int gameweek) {
        log.info("Processing red card for player: {} ({}), gameweek: {}", playerName, team, gameweek);

        Optional<Player> playerOpt = findPlayerByNameAndTeam(playerName, team);
        if (playerOpt.isEmpty()) {
            log.warn("Player not found: {} ({})", playerName, team);
            return;
        }

        Player player = playerOpt.get();
        updatePlayerStats(player, gameweek, EventType.RED_CARD, RED_CARD_POINTS);
        updateAllUserTeamPoints();

        log.info("Red card processed: {} lost {} points", playerName, Math.abs(RED_CARD_POINTS));
    }

    @Transactional
    public void processCleanSheet(String team, int gameweek) {
        log.info("Processing clean sheet for team: {}, gameweek: {}", team, gameweek);

        List<Player> teamPlayers = playerRepository.findByRealTeam(team);

        for (Player player : teamPlayers) {
            int points = switch (player.getPosition()) {
                case GK, DEF -> CLEAN_SHEET_GK_DEF_POINTS;
                case MID -> CLEAN_SHEET_MID_POINTS;
                case FWD -> 0; // Forwards don't get clean sheet points
            };

            if (points > 0) {
                // Check if player actually played (has stats for this gameweek)
                Optional<PlayerGameweekStats> existingStats = statsRepository
                        .findByPlayerIdAndGameweekNumber(player.getId(), gameweek);

                if (existingStats.isPresent()) {
                    PlayerGameweekStats stats = existingStats.get();
                    stats.setPointsEarned(stats.getPointsEarned() + points);
                    statsRepository.save(stats);

                    // Update player total points
                    player.setTotalPoints(player.getTotalPoints() + points);
                    playerRepository.save(player);
                }
            }
        }

        updateAllUserTeamPoints();
        log.info("Clean sheet processed for team: {}", team);
    }

    @Transactional
    public void processMinutesPlayed(String playerName, String team, int gameweek, int minutes) {
        log.info("Processing minutes played for player: {} ({}), gameweek: {}, minutes: {}", playerName, team, gameweek,
                minutes);

        Optional<Player> playerOpt = findPlayerByNameAndTeam(playerName, team);
        if (playerOpt.isEmpty()) {
            return;
        }

        int points = 0;
        if (minutes >= 70) {
            points = 2;
        } else if (minutes > 0) {
            points = 1;
        }

        if (points > 0) {
            Player player = playerOpt.get();
            updatePlayerStats(player, gameweek, EventType.APPEARANCE, points);

            // Note: Cannot easily update minutes field through generic updatePlayerStats
            // without changing its signature,
            // so let's do an explicit set if we needed to, but EventType mapping approach
            // can just be used to record points.
            statsRepository.findByPlayerIdAndGameweekNumber(player.getId(), gameweek).ifPresent(stats -> {
                stats.setMinutesPlayed(stats.getMinutesPlayed() + minutes);
                statsRepository.save(stats);
            });

            updateAllUserTeamPoints();
        }
    }

    @Transactional
    public void updateUserTeamPoints(Long userId) {
        userTeamRepository.findByUserId(userId).ifPresent(userTeam -> {
            int totalPoints = userTeam.getTeamPlayers().stream()
                    .mapToInt(tp -> tp.getPlayer().getTotalPoints())
                    .sum();

            userTeam.setTotalPoints(totalPoints);
            userTeamRepository.save(userTeam);

            log.info("Updated team points for user {}: {} points", userId, totalPoints);
        });
    }

    private void updatePlayerStats(Player player, int gameweek, EventType eventType, int points) {
        // Find or create gameweek stats
        PlayerGameweekStats stats = statsRepository
                .findByPlayerIdAndGameweekNumber(player.getId(), gameweek)
                .orElseGet(() -> {
                    PlayerGameweekStats newStats = PlayerGameweekStats.builder()
                            .player(player)
                            .gameweekNumber(gameweek)
                            .pointsEarned(0)
                            .goals(0)
                            .assists(0)
                            .yellowCards(0)
                            .redCards(0)
                            .minutesPlayed(0)
                            .build();
                    return newStats;
                });

        // Update stats based on event type
        switch (eventType) {
            case GOAL -> stats.setGoals(stats.getGoals() + 1);
            case ASSIST -> stats.setAssists(stats.getAssists() + 1);
            case YELLOW_CARD -> stats.setYellowCards(stats.getYellowCards() + 1);
            case RED_CARD -> stats.setRedCards(stats.getRedCards() + 1);
        }

        // Add points
        stats.setPointsEarned(stats.getPointsEarned() + points);
        statsRepository.save(stats);

        // Update player total points
        player.setTotalPoints(player.getTotalPoints() + points);
        playerRepository.save(player);
    }

    private void updateAllUserTeamPoints() {
        List<UserTeam> allTeams = userTeamRepository.findAll();

        for (UserTeam team : allTeams) {
            int totalPoints = team.getTeamPlayers().stream()
                    .filter(tp -> tp.isStarter()) // Only count Starting XI (null-safe: defaults to true)
                    .mapToInt(tp -> tp.getPlayer().getTotalPoints())
                    .sum();

            team.setTotalPoints(totalPoints);
        }

        userTeamRepository.saveAll(allTeams);
    }

    private Optional<Player> findPlayerByNameAndTeam(String playerName, String team) {
        List<Player> players = playerRepository.findByRealTeam(team);

        // Try exact/contains match first
        Optional<Player> exactMatch = players.stream()
                .filter(p -> p.getName().equalsIgnoreCase(playerName) ||
                        p.getName().toLowerCase().contains(playerName.toLowerCase()) ||
                        playerName.toLowerCase().contains(p.getName().toLowerCase()))
                .findFirst();

        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // If no direct match, try matching by last name (e.g. "A. El Ghazi" vs "Anwar
        // El Ghazi")
        String[] targetParts = playerName.trim().split("\\s+");
        if (targetParts.length > 0) {
            String targetLastName = targetParts[targetParts.length - 1].toLowerCase();

            return players.stream()
                    .filter(p -> {
                        String[] dbParts = p.getName().trim().split("\\s+");
                        if (dbParts.length > 0) {
                            String dbLastName = dbParts[dbParts.length - 1].toLowerCase();
                            return dbLastName.equals(targetLastName) || dbLastName.contains(targetLastName)
                                    || targetLastName.contains(dbLastName);
                        }
                        return false;
                    })
                    .findFirst();
        }

        return Optional.empty();
    }
}
