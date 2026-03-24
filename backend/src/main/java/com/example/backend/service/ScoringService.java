package com.example.backend.service;

import com.example.backend.model.entity.*;
import com.example.backend.model.enums.EventType;
import com.example.backend.model.enums.Position;
import com.example.backend.repository.MatchRepository;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.repository.PlayerGameweekStatsRepository;
import com.example.backend.repository.PlayerRepository;
import com.example.backend.repository.UserTeamGameweekPointsRepository;
import com.example.backend.repository.UserTeamGameweekLineupRepository;
import com.example.backend.repository.UserTeamGameweekTransfersRepository;
import com.example.backend.repository.UserTeamRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final PlayerRepository playerRepository;
    private final PlayerGameweekStatsRepository statsRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserTeamGameweekPointsRepository userTeamGameweekPointsRepository;
    private final UserTeamGameweekLineupRepository userTeamGameweekLineupRepository;
    private final UserTeamGameweekTransfersRepository userTeamGameweekTransfersRepository;
    private final GameweekRepository gameweekRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

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
        updateAllUserTeamPoints(gameweek);

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
        updateAllUserTeamPoints(gameweek);

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
        updateAllUserTeamPoints(gameweek);

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
        updateAllUserTeamPoints(gameweek);

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

        updateAllUserTeamPoints(gameweek);
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

            updateAllUserTeamPoints(gameweek);
        }
    }

    @Transactional
    public void updateUserTeamPoints(Long userId) {
        userTeamRepository.findByUserId(userId).ifPresent(userTeam -> {
            syncTeamGameweekPoints(userTeam);
            int totalPoints = userTeamGameweekPointsRepository.findByTeamId(userTeam.getId()).stream()
                    .mapToInt(UserTeamGameweekPoints::getPoints)
                    .sum();

            userTeam.setTotalPoints(totalPoints);
            userTeamRepository.save(userTeam);

            log.info("Updated team points for user {}: {} points", userId, totalPoints);
        });
    }

    private void updatePlayerStats(Player player, int gameweek, EventType eventType, int points) {
        Gameweek gameweekEntity = gameweekRepository.findByGameweekNumber(gameweek)
                .orElse(null);

        // Find or create gameweek stats
        PlayerGameweekStats stats = statsRepository
                .findByPlayerIdAndGameweekNumber(player.getId(), gameweek)
                .orElseGet(() -> {
                    PlayerGameweekStats newStats = PlayerGameweekStats.builder()
                            .player(player)
                            .gameweek(gameweekEntity)
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

        // Backfill relation for previously created rows that didn't set gameweek.
        if (stats.getGameweek() == null && gameweekEntity != null) {
            stats.setGameweek(gameweekEntity);
        }

        // Update stats based on event type
        switch (eventType) {
            case GOAL -> stats.setGoals(stats.getGoals() + 1);
            case ASSIST -> stats.setAssists(stats.getAssists() + 1);
            case YELLOW_CARD -> stats.setYellowCards(stats.getYellowCards() + 1);
            case RED_CARD -> stats.setRedCards(stats.getRedCards() + 1);
            case APPEARANCE -> {
            }
        }

        // Add points
        stats.setPointsEarned(stats.getPointsEarned() + points);
        statsRepository.save(stats);

        // Update player total points
        player.setTotalPoints(player.getTotalPoints() + points);
        playerRepository.save(player);
    }

    private void updateAllUserTeamPoints() {
        updateAllUserTeamPoints(null);
    }

    private void updateAllUserTeamPoints(Integer snapshotGameweek) {
        List<UserTeam> allTeams = userTeamRepository.findAll();

        for (UserTeam team : allTeams) {
            syncTeamGameweekPoints(team);
            if (snapshotGameweek != null) {
                snapshotTeamLineupForGameweek(team, snapshotGameweek);
            }
            int totalPoints = userTeamGameweekPointsRepository.findByTeamId(team.getId()).stream()
                    .mapToInt(UserTeamGameweekPoints::getPoints)
                    .sum();

            team.setTotalPoints(totalPoints);
        }

        userTeamRepository.saveAll(allTeams);
    }

    private void snapshotTeamLineupForGameweek(UserTeam team, int gameweekNumber) {
        if (team.getId() == null) {
            return;
        }

        Gameweek gameweek = gameweekRepository.findByGameweekNumber(gameweekNumber).orElse(null);

        UserTeamGameweekLineup snapshot = userTeamGameweekLineupRepository
                .findByTeamIdAndGameweekNumber(team.getId(), gameweekNumber)
                .orElseGet(() -> UserTeamGameweekLineup.builder()
                        .team(team)
                        .gameweek(gameweek)
                        .gameweekNumber(gameweekNumber)
                        .build());

        if (snapshot.getGameweek() == null && gameweek != null) {
            snapshot.setGameweek(gameweek);
        }

        snapshot.setCapturedAt(LocalDateTime.now());
        if (snapshot.getId() != null && !snapshot.getPlayers().isEmpty()) {
            snapshot.getPlayers().clear();
            userTeamGameweekLineupRepository.saveAndFlush(snapshot);
        } else {
            snapshot.getPlayers().clear();
        }

        Set<Long> seenPlayerIds = new HashSet<>();

        int teamPoints = 0;
        for (UserTeamPlayer teamPlayer : team.getTeamPlayers()) {
            if (!seenPlayerIds.add(teamPlayer.getPlayer().getId())) {
                continue;
            }

            int points = calculatePlayerGameweekPointsDeduplicated(teamPlayer.getPlayer().getId(), gameweekNumber);

            UserTeamGameweekLineupPlayer playerRow = UserTeamGameweekLineupPlayer.builder()
                    .lineup(snapshot)
                    .player(teamPlayer.getPlayer())
                    .starter(teamPlayer.isStarter())
                    .pointsEarned(points)
                    .build();

            snapshot.getPlayers().add(playerRow);

            if (teamPlayer.isStarter()) {
                teamPoints += points;
            }
        }

        snapshot.setTeamPoints(teamPoints);
        userTeamGameweekLineupRepository.save(snapshot);
    }

    private int calculatePlayerGameweekPointsDeduplicated(Long playerId, int gameweekNumber) {
        List<PlayerGameweekStats> rows = statsRepository.findAllByPlayerIdAndGameweekNumber(playerId, gameweekNumber);
        if (rows.isEmpty()) {
            return 0;
        }

        // Keep only the latest stat row for each match to guard against accidental duplicates.
        Map<String, PlayerGameweekStats> byKey = rows.stream()
                .sorted((a, b) -> Long.compare(
                        b.getId() == null ? 0L : b.getId(),
                        a.getId() == null ? 0L : a.getId()))
                .collect(Collectors.toMap(
                        row -> row.getMatch() != null
                                ? "M-" + row.getMatch().getId()
                                : "R-" + row.getId(),
                        row -> row,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new));

        return byKey.values().stream()
                .mapToInt(r -> r.getPointsEarned() == null ? 0 : r.getPointsEarned())
                .sum();
    }

    public void syncTeamGameweekPoints(UserTeam team) {
        if (team.getId() == null) {
            return;
        }

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        List<Gameweek> allGameweeks = gameweekRepository.findAll().stream()
            .sorted(Comparator.comparingInt(Gameweek::getGameweekNumber))
            .toList();

        Set<Integer> startedGameweeks = allGameweeks.stream()
            .filter(gw -> !gw.getStartDate().isAfter(nowUtc))
            .map(Gameweek::getGameweekNumber)
            .collect(Collectors.toSet());

        Map<Integer, Integer> pointsByGameweek = team.getTeamPlayers().stream()
                .filter(UserTeamPlayer::isStarter)
                .flatMap(tp -> statsRepository.findByPlayerId(tp.getPlayer().getId()).stream())
            .filter(stats -> startedGameweeks.contains(stats.getGameweekNumber()))
                .collect(Collectors.groupingBy(
                        PlayerGameweekStats::getGameweekNumber,
                        Collectors.summingInt(PlayerGameweekStats::getPointsEarned)));

        List<UserTeamGameweekPoints> existingRows = userTeamGameweekPointsRepository.findByTeamId(team.getId());
        Map<Integer, UserTeamGameweekPoints> existingByGw = existingRows.stream()
                .collect(Collectors.toMap(UserTeamGameweekPoints::getGameweekNumber, r -> r, (a, b) -> a));

        for (Gameweek gw : allGameweeks) {
            int gwNumber = gw.getGameweekNumber();
            int points = pointsByGameweek.getOrDefault(gwNumber, 0);

            // Apply transfer penalty if transfers were made in this gameweek
            Optional<UserTeamGameweekTransfers> transferRecord = userTeamGameweekTransfersRepository
                    .findByTeamIdAndGameweekNumber(team.getId(), gwNumber);
            
            if (transferRecord.isPresent()) {
                int transferCount = transferRecord.get().getTransferCount();
                int penaltyPoints = calculateTransferPenalty(transferCount);
                points = points + penaltyPoints; // penaltyPoints is negative
                log.debug("Applied transfer penalty of {} pts ({}  transfers) to team {} GW {}", 
                        penaltyPoints, transferCount, team.getId(), gwNumber);
            }

            UserTeamGameweekPoints row = existingByGw.get(gwNumber);
            if (row == null) {
                row = UserTeamGameweekPoints.builder()
                        .team(team)
                        .gameweek(gw)
                        .gameweekNumber(gwNumber)
                        .points(points)
                        .build();
            } else {
                row.setPoints(points);
                if (row.getGameweek() == null) {
                    row.setGameweek(gw);
                }
            }
            userTeamGameweekPointsRepository.save(row);
        }
        }

    private int calculateTransferPenalty(int transferCount) {
        // 1 free transfer, each additional transfer costs -4 points
        if (transferCount <= 1) {
            return 0;
        }
        return -(transferCount - 1) * 4;
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

    @Transactional
    public void ensureGameweekStatsInitialized(int gameweekNumber) {
        Optional<Gameweek> gwOpt = gameweekRepository.findByGameweekNumber(gameweekNumber);
        if (gwOpt.isEmpty()) {
            return;
        }

        List<DataInitializationService.MatchJsonDTO> allMatches;
        try {
            ClassPathResource resource = new ClassPathResource("data/fique_data");
            allMatches = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<List<DataInitializationService.MatchJsonDTO>>() {
                    });
        } catch (IOException e) {
            log.error("Failed to load fixture data for gameweek {} backfill", gameweekNumber, e);
            return;
        }

        List<DataInitializationService.MatchJsonDTO> gameweekMatches = allMatches.stream()
                .filter(m -> m.getGameweek() != null && m.getGameweek() == gameweekNumber)
                .toList();

        if (gameweekMatches.isEmpty()) {
            return;
        }

        Map<String, Match> matchByKey = new HashMap<>();
        matchRepository.findByGameweekId(gwOpt.get().getId()).forEach(m -> {
            matchByKey.put(matchKey(m.getHomeTeam(), m.getAwayTeam()), m);
        });

        // Only score matches that are truly finished and not already persisted.
        List<DataInitializationService.MatchJsonDTO> finishedUnprocessedMatches = gameweekMatches.stream()
                .filter(mj -> {
                    Match dbMatch = matchByKey.get(matchKey(mj.getHomeTeam(), mj.getAwayTeam()));
                    return dbMatch != null
                            && isMatchFinishedForScoring(dbMatch)
                            && statsRepository.findByMatchId(dbMatch.getId()).isEmpty();
                })
                .toList();

        if (finishedUnprocessedMatches.isEmpty()) {
            return;
        }

        Map<Long, PlayerAccumulator> accum = new HashMap<>();

        for (DataInitializationService.MatchJsonDTO matchJson : finishedUnprocessedMatches) {
            applyLineup(accum, matchJson.getLineups() != null ? matchJson.getLineups().getHome() : null,
                    matchJson.getHomeTeam(), matchByKey.get(matchKey(matchJson.getHomeTeam(), matchJson.getAwayTeam())));
            applyLineup(accum, matchJson.getLineups() != null ? matchJson.getLineups().getAway() : null,
                    matchJson.getAwayTeam(), matchByKey.get(matchKey(matchJson.getHomeTeam(), matchJson.getAwayTeam())));

            if (matchJson.getEvents() != null) {
                applyEvents(accum, matchJson.getEvents().getGoals(), matchJson.getHomeTeam(), EventType.GOAL);
                applyEvents(accum, matchJson.getEvents().getGoals(), matchJson.getAwayTeam(), EventType.GOAL);

                applyEvents(accum, matchJson.getEvents().getAssists(), matchJson.getHomeTeam(), EventType.ASSIST);
                applyEvents(accum, matchJson.getEvents().getAssists(), matchJson.getAwayTeam(), EventType.ASSIST);

                applyEvents(accum, matchJson.getEvents().getYellowCards(), matchJson.getHomeTeam(),
                        EventType.YELLOW_CARD);
                applyEvents(accum, matchJson.getEvents().getYellowCards(), matchJson.getAwayTeam(),
                        EventType.YELLOW_CARD);

                applyEvents(accum, matchJson.getEvents().getRedCards(), matchJson.getHomeTeam(), EventType.RED_CARD);
                applyEvents(accum, matchJson.getEvents().getRedCards(), matchJson.getAwayTeam(), EventType.RED_CARD);
            }

            applyCleanSheet(accum, matchJson.getHomeTeam(), matchJson.getAwayScore() == null ? 0 : matchJson.getAwayScore());
            applyCleanSheet(accum, matchJson.getAwayTeam(), matchJson.getHomeScore() == null ? 0 : matchJson.getHomeScore());
        }

        for (PlayerAccumulator a : accum.values()) {
            int appearancePoints = a.minutesPlayed >= 70 ? 2 : (a.minutesPlayed > 0 ? 1 : 0);
            int points = (a.goals * a.player.getGoalPoints())
                    + (a.assists * ASSIST_POINTS)
                    + (a.yellowCards * YELLOW_CARD_POINTS)
                    + (a.redCards * RED_CARD_POINTS)
                    + appearancePoints
                    + a.cleanSheetPoints;

            PlayerGameweekStats stats = PlayerGameweekStats.builder()
                    .player(a.player)
                    .gameweek(gwOpt.get())
                    .match(a.match)
                    .gameweekNumber(gameweekNumber)
                    .minutesPlayed(a.minutesPlayed)
                    .goals(a.goals)
                    .assists(a.assists)
                    .yellowCards(a.yellowCards)
                    .redCards(a.redCards)
                    .pointsEarned(points)
                    .build();

            statsRepository.save(stats);
        }

        // Recompute persisted player totals from all recorded gameweek stats.
        for (PlayerAccumulator a : accum.values()) {
            int total = statsRepository.findByPlayerId(a.player.getId()).stream()
                    .mapToInt(PlayerGameweekStats::getPointsEarned)
                    .sum();
            a.player.setTotalPoints(total);
        }
        playerRepository.saveAll(accum.values().stream().map(pa -> pa.player).toList());

        updateAllUserTeamPoints();
        log.info("Backfilled player gameweek stats for gameweek {} ({} players) across {} finished matches",
                gameweekNumber, accum.size(), finishedUnprocessedMatches.size());
    }

    private boolean isMatchFinishedForScoring(Match match) {
        if (Boolean.TRUE.equals(match.getFinished())) {
            return true;
        }
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        return !match.getKickoffTime().plusMinutes(105).isAfter(nowUtc);
    }

    private void applyLineup(Map<Long, PlayerAccumulator> accum, DataInitializationService.LineupJsonDTO lineup,
            String team,
            Match match) {
        if (lineup == null) {
            return;
        }
        applyLineupPlayers(accum, lineup.getStarting(), team, match);
        applyLineupPlayers(accum, lineup.getSubstitutesIn(), team, match);
        applyLineupPlayers(accum, lineup.getBenchUnused(), team, match);
    }

    private void applyLineupPlayers(Map<Long, PlayerAccumulator> accum, List<DataInitializationService.PlayerJsonDTO> players,
            String team, Match match) {
        if (players == null) {
            return;
        }

        for (DataInitializationService.PlayerJsonDTO p : players) {
            Optional<Player> playerOpt = findPlayerByNameAndTeam(p.getName(), team);
            if (playerOpt.isEmpty()) {
                continue;
            }

            Player player = playerOpt.get();
            PlayerAccumulator a = accum.computeIfAbsent(player.getId(), id -> new PlayerAccumulator(player));
            a.minutesPlayed += p.getMinutes() == null ? 0 : p.getMinutes();
            if (a.match == null) {
                a.match = match;
            }
        }
    }

    private void applyEvents(Map<Long, PlayerAccumulator> accum, List<DataInitializationService.EventPlayerJsonDTO> events,
            String team,
            EventType type) {
        if (events == null) {
            return;
        }

        for (DataInitializationService.EventPlayerJsonDTO e : events) {
            if (e.getTeam() == null || !e.getTeam().equalsIgnoreCase(team)) {
                continue;
            }
            Optional<Player> playerOpt = findPlayerByNameAndTeam(e.getPlayer(), team);
            if (playerOpt.isEmpty()) {
                continue;
            }

            PlayerAccumulator a = accum.computeIfAbsent(playerOpt.get().getId(), id -> new PlayerAccumulator(playerOpt.get()));
            switch (type) {
                case GOAL -> a.goals++;
                case ASSIST -> a.assists++;
                case YELLOW_CARD -> a.yellowCards++;
                case RED_CARD -> a.redCards++;
                default -> {
                }
            }
        }
    }

    private void applyCleanSheet(Map<Long, PlayerAccumulator> accum, String team, int goalsConceded) {
        if (goalsConceded != 0) {
            return;
        }

        for (PlayerAccumulator a : accum.values()) {
            if (!a.player.getRealTeam().equalsIgnoreCase(team)) {
                continue;
            }
            if (a.minutesPlayed <= 0) {
                continue;
            }

            if (a.player.getPosition() == Position.GK || a.player.getPosition() == Position.DEF) {
                a.cleanSheetPoints += CLEAN_SHEET_GK_DEF_POINTS;
            } else if (a.player.getPosition() == Position.MID) {
                a.cleanSheetPoints += CLEAN_SHEET_MID_POINTS;
            }
        }
    }

    private String matchKey(String home, String away) {
        return (home == null ? "" : home.trim().toLowerCase()) + "|" + (away == null ? "" : away.trim().toLowerCase());
    }

    private static class PlayerAccumulator {
        private final Player player;
        private Match match;
        private int minutesPlayed;
        private int goals;
        private int assists;
        private int yellowCards;
        private int redCards;
        private int cleanSheetPoints;

        private PlayerAccumulator(Player player) {
            this.player = player;
        }
    }
}
