package com.example.backend.service;

import com.example.backend.dto.MatchEventDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Loads match events from fique_data JSON and assigns each event a simulated
 * match-minute so they can be revealed progressively during live simulation.
 *
 * Events are cached in memory keyed by "{homeTeam}_{awayTeam}_{gameweek}".
 *
 * Minute assignment strategy (deterministic, no random):
 * - Goals / Assists: spread evenly across [10, 88], offset by event index
 * - Yellow cards: spread evenly across [15, 85]
 * - Red cards: spread evenly across [55, 88]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchEventCacheService {

    private final ObjectMapper objectMapper;

    /** Key: homeTeam_awayTeam_gameweekNumber → ordered events */
    private final Map<String, List<MatchEventDTO>> cache = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource("data/fique_data");
            List<RawMatch> matches = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<RawMatch>>() {
                    });

            for (RawMatch m : matches) {
                String key = m.homeTeam + "_" + m.awayTeam + "_" + m.gameweek;
                List<MatchEventDTO> events = buildEvents(m);
                cache.put(key, events);
            }
            log.info("MatchEventCacheService: loaded events for {} matches", cache.size());
        } catch (IOException e) {
            log.error("Failed to load match events", e);
        }
    }

    /**
     * Returns the event list for a match, or an empty list if not found.
     */
    public List<MatchEventDTO> getEvents(String homeTeam, String awayTeam, int gameweek) {
        return cache.getOrDefault(homeTeam + "_" + awayTeam + "_" + gameweek, List.of());
    }

    // ------------------------------------------------------------------
    private List<MatchEventDTO> buildEvents(RawMatch m) {
        List<MatchEventDTO> events = new ArrayList<>();
        if (m.events == null)
            return events;

        // --- Goals (and parallel assists at same minute - 1) ---
        List<RawEvent> goals = safe(m.events.goals);
        List<RawEvent> assists = safe(m.events.assists);
        int goalCount = goals.size();

        for (int i = 0; i < goalCount; i++) {
            int min = spreadMinute(10, 88, i, goalCount, m.id * 3L);
            events.add(new MatchEventDTO("GOAL", goals.get(i).player, goals.get(i).team, min));
            if (i < assists.size()) {
                // Assist fires at same minute
                events.add(new MatchEventDTO("ASSIST", assists.get(i).player, assists.get(i).team, min));
            }
        }

        // --- Yellow cards ---
        List<RawEvent> yellows = safe(m.events.yellowCards);
        for (int i = 0; i < yellows.size(); i++) {
            int min = spreadMinute(15, 85, i, Math.max(yellows.size(), 1), m.id * 7L);
            events.add(new MatchEventDTO("YELLOW_CARD", yellows.get(i).player, yellows.get(i).team, min));
        }

        // --- Red cards ---
        List<RawEvent> reds = safe(m.events.redCards);
        for (int i = 0; i < reds.size(); i++) {
            int min = spreadMinute(55, 88, i, Math.max(reds.size(), 1), m.id * 11L);
            events.add(new MatchEventDTO("RED_CARD", reds.get(i).player, reds.get(i).team, min));
        }

        // Sort by minute
        events.sort(Comparator.comparingInt(MatchEventDTO::minute));
        return events;
    }

    /** Deterministically spread event index i across [low, high] range */
    private int spreadMinute(int low, int high, int index, int total, long seed) {
        int range = high - low;
        // Base spread
        int base = low + (int) ((long) range * index / Math.max(total, 1));
        // Small deterministic offset per match so events don't all land at same minute
        int offset = (int) (Math.abs(seed + index * 13) % 8);
        return Math.min(high, base + offset);
    }

    private List<RawEvent> safe(List<RawEvent> list) {
        return list == null ? List.of() : list;
    }

    // ---- Inner JSON DTO classes ----
    @Data
    static class RawMatch {
        private int id;
        private int gameweek;
        @JsonProperty("home_team")
        private String homeTeam;
        @JsonProperty("away_team")
        private String awayTeam;
        private RawEvents events;
    }

    @Data
    static class RawEvents {
        private List<RawEvent> goals;
        private List<RawEvent> assists;
        @JsonProperty("yellow_cards")
        private List<RawEvent> yellowCards;
        @JsonProperty("red_cards")
        private List<RawEvent> redCards;
    }

    @Data
    static class RawEvent {
        private String player;
        private String team;
    }
}
