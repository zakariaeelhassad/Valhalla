package com.example.backend.service.Impl;

import com.example.backend.dto.match.CurrentGameweekResponse;
import com.example.backend.dto.match.GameweekSummaryResponse;
import com.example.backend.dto.match.MatchEventDTO;
import com.example.backend.dto.match.MatchStatusResponse;
import com.example.backend.model.entity.Gameweek;
import com.example.backend.model.entity.Match;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.repository.MatchRepository;
import com.example.backend.service.MatchService;
import com.example.backend.service.MatchEventCacheService;
import com.example.backend.service.SimulatedClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final GameweekRepository gameweekRepository;
    private final SimulatedClockService clockService;
    private final MatchEventCacheService eventCache;

    @Override
    public Optional<List<MatchStatusResponse>> getByGameweek(Integer gameweekNumber) {
        Optional<Gameweek> gwOpt = gameweekRepository.findByGameweekNumber(gameweekNumber);
        if (gwOpt.isEmpty()) {
            return Optional.empty();
        }

        List<MatchStatusResponse> response = matchRepository.findByGameweekId(gwOpt.get().getId()).stream()
                .sorted(Comparator.comparing(Match::getKickoffTime))
                .map(m -> toDto(m, gameweekNumber))
                .collect(Collectors.toList());

        return Optional.of(response);
    }

    @Override
    public List<MatchStatusResponse> getLive() {
        return matchRepository.findAll().stream()
                .map(m -> toDto(m, m.getGameweek().getGameweekNumber()))
                .filter(m -> "LIVE".equals(m.status()))
                .sorted(Comparator.comparing(MatchStatusResponse::kickoffTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<GameweekSummaryResponse> getGameweeks() {
        List<Gameweek> gws = gameweekRepository.findAll();
        gws.sort(Comparator.comparingInt(Gameweek::getGameweekNumber));
        LocalDateTime now = clockService.getSimulatedNow();

        return gws.stream().map(gw -> {
            List<Match> matches = matchRepository.findByGameweekId(gw.getId());
            long liveCount = matches.stream().filter(m -> "LIVE"
                    .equals(clockService.computeStatus(m.getKickoffTime(), m.getFinished()))).count();
            long finishedCount = matches.stream().filter(m -> "FINISHED"
                    .equals(clockService.computeStatus(m.getKickoffTime(), m.getFinished()))).count();
            String gwStatus = liveCount > 0 ? "LIVE"
                    : finishedCount == matches.size() ? "COMPLETED"
                            : gw.getStartDate().isAfter(now) ? "UPCOMING" : "ACTIVE";
            return new GameweekSummaryResponse(gw.getGameweekNumber(), matches.size(), (int) liveCount,
                    (int) finishedCount, gwStatus);
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getClock() {
        return Map.of("simulatedNow", clockService.getSimulatedNow().toString());
    }

    @Override
    public CurrentGameweekResponse getCurrentGameweekFromDb() {
        List<Gameweek> gameweeks = gameweekRepository.findAll();
        if (gameweeks.isEmpty()) {
            return new CurrentGameweekResponse(getDisplayNowUtc().toString(), null, List.of());
        }

        gameweeks.sort(Comparator.comparingInt(Gameweek::getGameweekNumber));
        LocalDateTime now = getDisplayNowUtc();

        Optional<Gameweek> active = gameweeks.stream()
                .filter(gw -> !gw.getStartDate().isAfter(now) && !gw.getEndDate().isBefore(now))
                .findFirst();

        Gameweek currentGw = active.orElseGet(() -> gameweeks.stream()
                .filter(gw -> gw.getStartDate().isAfter(now))
                .findFirst()
                .orElse(gameweeks.get(gameweeks.size() - 1)));

        List<MatchStatusResponse> matches = matchRepository.findByGameweekId(currentGw.getId()).stream()
                .sorted(Comparator.comparing(Match::getKickoffTime))
                .map(m -> toDto(m, currentGw.getGameweekNumber()))
                .collect(Collectors.toList());

        return new CurrentGameweekResponse(now.toString(), currentGw.getGameweekNumber(), matches);
    }

    private MatchStatusResponse toDto(Match m, int gwNumber) {
        String status = computeDisplayStatus(m.getKickoffTime(), Boolean.TRUE.equals(m.getFinished()));
        int elapsed = "LIVE".equals(status) ? getDisplayElapsedMinutes(m.getKickoffTime()) : 0;
        List<MatchEventDTO> events = eventCache.getEvents(m.getHomeTeam(), m.getAwayTeam(), gwNumber);
        return new MatchStatusResponse(
                m.getId(), gwNumber,
                m.getHomeTeam(), m.getAwayTeam(),
                m.getHomeScore(), m.getAwayScore(),
                m.getKickoffTime(), m.getFinished(),
                status, elapsed, events);
    }

    private String computeDisplayStatus(LocalDateTime kickoffTime, boolean finished) {
        LocalDateTime now = getDisplayNowUtc();

        if (finished) {
            return "FINISHED";
        }

        if (now.isBefore(kickoffTime)) {
            return "SCHEDULED";
        }

        LocalDateTime matchEnd = kickoffTime.plusMinutes(105);
        if (now.isBefore(matchEnd)) {
            return "LIVE";
        }

        return "FINISHED";
    }

    private int getDisplayElapsedMinutes(LocalDateTime kickoffTime) {
        LocalDateTime now = getDisplayNowUtc();
        if (now.isBefore(kickoffTime)) {
            return 0;
        }
        long secs = java.time.Duration.between(kickoffTime, now).getSeconds();
        return (int) Math.min(secs / 60, 90);
    }

    private LocalDateTime getDisplayNowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}