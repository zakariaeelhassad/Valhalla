package com.example.backend.service.Impl;

import com.example.backend.dto.game.TransferWindowStatusResponse;
import com.example.backend.model.entity.Gameweek;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.service.TransferWindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransferWindowServiceImpl implements TransferWindowService {

    private final GameweekRepository gameweekRepository;
        private volatile List<Gameweek> cachedGameweeks = List.of();
        private volatile long cacheLoadedAtMs = 0L;
        private static final long CACHE_TTL_MS = 30_000L;

        public TransferWindowStatusResponse getTransferWindowStatus() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

                List<Gameweek> gameweeks = getGameweeks();

        if (gameweeks.isEmpty()) {
            return new TransferWindowStatusResponse(
                    now.toString(),
                    null,
                    null,
                    null,
                    false,
                    "NO_GAMEWEEKS",
                    "No gameweeks are available in the database.");
        }

        Optional<Gameweek> activeGameweek = gameweeks.stream()
                .filter(gw -> !gw.getStartDate().isAfter(now) && !gw.getEndDate().isBefore(now))
                .findFirst();

        if (activeGameweek.isPresent()) {
            Gameweek active = activeGameweek.get();
            Optional<Gameweek> next = gameweeks.stream()
                    .filter(gw -> gw.getGameweekNumber() > active.getGameweekNumber())
                    .findFirst();

            return new TransferWindowStatusResponse(
                    now.toString(),
                    active.getGameweekNumber(),
                    next.map(Gameweek::getGameweekNumber).orElse(null),
                    next.map(gw -> gw.getStartDate().toString()).orElse(null),
                    false,
                    "ACTIVE",
                    "We are currently in Gameweek " + active.getGameweekNumber() + ". Transfers are locked.");
        }

        Optional<Gameweek> upcomingGameweek = gameweeks.stream()
                .filter(gw -> gw.getStartDate().isAfter(now))
                .findFirst();

        if (upcomingGameweek.isPresent()) {
            Gameweek next = upcomingGameweek.get();
            Optional<Gameweek> latestCompleted = gameweeks.stream()
                    .filter(gw -> gw.getEndDate().isBefore(now))
                    .reduce((first, second) -> second);

            boolean isBeforeSeasonStart = latestCompleted.isEmpty();
            String phase = isBeforeSeasonStart ? "PRE_DEADLINE" : "BETWEEN_GAMEWEEKS";
            String message = isBeforeSeasonStart
                    ? "Gameweek " + next.getGameweekNumber() + " has not started yet. You can make changes until the deadline."
                    : "Gameweek " + latestCompleted.get().getGameweekNumber() + " has finished. Gameweek "
                            + next.getGameweekNumber() + " will start soon.";

            return new TransferWindowStatusResponse(
                    now.toString(),
                    null,
                    next.getGameweekNumber(),
                    next.getStartDate().toString(),
                    true,
                    phase,
                    message);
        }

        return new TransferWindowStatusResponse(
                now.toString(),
                null,
                null,
                null,
                false,
                "SEASON_FINISHED",
                "The season has finished. Transfers are closed.");
    }

    public boolean isTransfersAllowed() {
        return getTransferWindowStatus().transfersAllowed();
    }

        private List<Gameweek> getGameweeks() {
                long nowMs = System.currentTimeMillis();
                if ((nowMs - cacheLoadedAtMs) < CACHE_TTL_MS && !cachedGameweeks.isEmpty()) {
                        return cachedGameweeks;
                }

                synchronized (this) {
                        nowMs = System.currentTimeMillis();
                        if ((nowMs - cacheLoadedAtMs) < CACHE_TTL_MS && !cachedGameweeks.isEmpty()) {
                                return cachedGameweeks;
                        }

                        cachedGameweeks = gameweekRepository.findAll().stream()
                                        .sorted(Comparator.comparingInt(Gameweek::getGameweekNumber))
                                        .toList();
                        cacheLoadedAtMs = nowMs;
                        return cachedGameweeks;
                }
        }

}


