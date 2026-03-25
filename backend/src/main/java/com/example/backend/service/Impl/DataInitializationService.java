package com.example.backend.service.Impl;

import com.example.backend.model.entity.*;
import com.example.backend.model.enums.Position;
import com.example.backend.repository.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements com.example.backend.service.DataInitializationService, CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final GameweekRepository gameweekRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Data Initialization Started ===");

        // Parse JSON once
        ClassPathResource resource = new ClassPathResource("data/fique_data");
        List<MatchJsonDTO> matchJsonList = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<MatchJsonDTO>>() {
                });
        log.info("Parsed {} matches from fique_data", matchJsonList.size());

        try {
            if (playerRepository.count() == 0) {
                loadPlayers(matchJsonList);
            } else {
                log.info("Players already loaded, skipping.");
            }
            if (matchRepository.count() == 0) {
                loadGameweeksAndMatches(matchJsonList);
            } else {
                log.info("Matches already loaded, skipping.");
            }
            log.info("=== Data Initialization Completed Successfully ===");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
            throw e;
        }
    }

    // PLAYERS
    private void loadPlayers(List<MatchJsonDTO> matches) {
        log.info("Loading players...");
        Map<String, PlayerData> uniquePlayers = new HashMap<>();
        for (MatchJsonDTO match : matches) {
            if (match.getLineups() != null) {
                extractPlayersFromLineup(match.getLineups().getHome(), match.getHomeTeam(), uniquePlayers);
                extractPlayersFromLineup(match.getLineups().getAway(), match.getAwayTeam(), uniquePlayers);
            }
        }
        log.info("Found {} unique players", uniquePlayers.size());

        List<Player> players = uniquePlayers.values().stream()
                .map(pd -> Player.builder()
                        .name(pd.getName())
                        .position(pd.getPosition())
                        .realTeam(pd.getRealTeam())
                        .price(calculatePrice(pd.getPosition()))
                        .totalPoints(0)
                        .build())
                .collect(Collectors.toList());

        playerRepository.saveAll(players);
        log.info("Saved {} players", players.size());
    }

    // GAMEWEEKS + MATCHES
    private void loadGameweeksAndMatches(List<MatchJsonDTO> matchJsonList) {
        log.info("Loading gameweeks and matches...");

        // Group matches by gameweek number
        Map<Integer, List<MatchJsonDTO>> byGw = matchJsonList.stream()
                .collect(Collectors.groupingBy(MatchJsonDTO::getGameweek));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        for (Map.Entry<Integer, List<MatchJsonDTO>> entry : new TreeMap<>(byGw).entrySet()) {
            int gwNum = entry.getKey();
            List<MatchJsonDTO> gwMatches = entry.getValue();

            // Determine GW date range
            List<LocalDateTime> kickoffs = gwMatches.stream()
                    .map(m -> LocalDateTime.parse(m.getKickoffTime(), fmt))
                    .sorted()
                    .collect(Collectors.toList());

            LocalDateTime gwStart = kickoffs.get(0).minusHours(1);
            LocalDateTime gwEnd = kickoffs.get(kickoffs.size() - 1).plusHours(3);

            // Save Gameweek
            Gameweek gw = Gameweek.builder()
                    .gameweekNumber(gwNum)
                    .startDate(gwStart)
                    .endDate(gwEnd)
                    .status("UPCOMING")
                    .build();
            gw = gameweekRepository.save(gw);

            // Save Matches for this GW
            List<Match> matchEntities = new ArrayList<>();
            for (MatchJsonDTO mj : gwMatches) {
                LocalDateTime kickoff = LocalDateTime.parse(mj.getKickoffTime(), fmt);
                matchEntities.add(Match.builder()
                        .gameweek(gw)
                        .homeTeam(mj.getHomeTeam())
                        .awayTeam(mj.getAwayTeam())
                        .homeScore(mj.getHomeScore() != null ? mj.getHomeScore() : 0)
                        .awayScore(mj.getAwayScore() != null ? mj.getAwayScore() : 0)
                        .kickoffTime(kickoff)
                        .finished(true) // historical data: all results are final
                        .build());
            }
            matchRepository.saveAll(matchEntities);
        }

        log.info("Loaded {} gameweeks and {} matches", byGw.size(), matchJsonList.size());
    }

    // HELPERS
    private void extractPlayersFromLineup(LineupJsonDTO lineup, String teamName,
            Map<String, PlayerData> uniquePlayers) {
        if (lineup == null)
            return;
        addAll(lineup.getStarting(), teamName, uniquePlayers);
        addAll(lineup.getSubstitutesIn(), teamName, uniquePlayers);
        addAll(lineup.getBenchUnused(), teamName, uniquePlayers);
    }

    private void addAll(List<PlayerJsonDTO> list, String teamName,
            Map<String, PlayerData> uniquePlayers) {
        if (list == null)
            return;
        for (PlayerJsonDTO p : list) {
            String key = p.getName() + "_" + teamName;
            if (!uniquePlayers.containsKey(key)) {
                uniquePlayers.put(key, new PlayerData(p.getName(), parsePosition(p.getPosition()), teamName));
            }
        }
    }

    private Position parsePosition(String positionStr) {
        if (positionStr == null)
            return Position.MID;
        return switch (positionStr.toUpperCase()) {
            case "GK" -> Position.GK;
            case "DEF" -> Position.DEF;
            case "FWD" -> Position.FWD;
            default -> Position.MID;
        };
    }

    private BigDecimal calculatePrice(Position position) {
        Random random = new Random();
        double basePrice = switch (position) {
            case GK -> 4.5 + (random.nextDouble() * 2.0);
            case DEF -> 4.0 + (random.nextDouble() * 3.0);
            case MID -> 5.0 + (random.nextDouble() * 8.0);
            case FWD -> 5.5 + (random.nextDouble() * 7.5);
        };
        return BigDecimal.valueOf(Math.round(basePrice * 10.0) / 10.0);
    }

    // Inner DTO Classes for JSON Parsing

    @Data
    public static class MatchJsonDTO {
        private Integer id;
        private Integer gameweek;
        @JsonProperty("kickoff_time")
        private String kickoffTime;
        @JsonProperty("home_team")
        private String homeTeam;
        @JsonProperty("away_team")
        private String awayTeam;
        @JsonProperty("home_score")
        private Integer homeScore;
        @JsonProperty("away_score")
        private Integer awayScore;
        private EventsJsonDTO events;
        private LineupsJsonDTO lineups;
    }

    @Data
    public static class LineupsJsonDTO {
        private LineupJsonDTO home;
        private LineupJsonDTO away;
    }

    @Data
    public static class LineupJsonDTO {
        private List<PlayerJsonDTO> starting;
        @JsonProperty("substitutes_in")
        private List<PlayerJsonDTO> substitutesIn;
        @JsonProperty("bench_unused")
        private List<PlayerJsonDTO> benchUnused;
    }

    @Data
    public static class PlayerJsonDTO {
        private String name;
        private String position;
        private Integer minutes;
    }

    @Data
    public static class EventsJsonDTO {
        private List<EventPlayerJsonDTO> goals;
        private List<EventPlayerJsonDTO> assists;
        @JsonProperty("yellow_cards")
        private List<EventPlayerJsonDTO> yellowCards;
        @JsonProperty("red_cards")
        private List<EventPlayerJsonDTO> redCards;
    }

    @Data
    public static class EventPlayerJsonDTO {
        private String player;
        private String team;
    }

    @Data
    private static class PlayerData {
        private final String name;
        private final Position position;
        private final String realTeam;
    }
}


