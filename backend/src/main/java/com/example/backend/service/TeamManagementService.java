package com.example.backend.service;

import com.example.backend.exception.InsufficientBudgetException;
import com.example.backend.exception.InvalidTransferException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.TeamFullException;
import com.example.backend.model.entity.Player;
import com.example.backend.model.enums.Position;
import com.example.backend.model.entity.User;
import com.example.backend.model.entity.UserTeam;
import com.example.backend.model.entity.UserTeamGameweekLineup;
import com.example.backend.model.entity.UserTeamGameweekLineupPlayer;
import com.example.backend.model.entity.UserTeamPlayer;
import com.example.backend.model.entity.UserTeamGameweekTransfers;
import com.example.backend.model.entity.Gameweek;
import com.example.backend.model.entity.Match;
import com.example.backend.repository.PlayerRepository;
import com.example.backend.repository.PlayerGameweekStatsRepository;
import com.example.backend.repository.GameweekRepository;
import com.example.backend.repository.MatchRepository;
import com.example.backend.repository.UserTeamGameweekPointsRepository;
import com.example.backend.repository.UserTeamGameweekLineupRepository;
import com.example.backend.repository.UserTeamGameweekTransfersRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserTeamPlayerRepository;
import com.example.backend.repository.UserTeamRepository;
import com.example.backend.dto.game.GameweekStatsResponse;
import com.example.backend.dto.game.GameweekStatsResponse.PlayerGameweekScore;
import com.example.backend.dto.game.GameweekTotalPointsResponse;
import com.example.backend.dto.team.LineupPlayerMeta;
import com.example.backend.dto.team.TeamLineupPlayerResponse;
import com.example.backend.dto.team.TeamLineupResponse;
import com.example.backend.model.entity.PlayerGameweekStats;
import com.example.backend.model.entity.UserTeamGameweekPoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamManagementService {

        private final UserTeamRepository userTeamRepository;
        private final PlayerRepository playerRepository;
        private final UserRepository userRepository;
        private final UserTeamPlayerRepository userTeamPlayerRepository;
        private final TransferWindowService transferWindowService;
        private final PlayerGameweekStatsRepository statsRepository;
        private final MatchRepository matchRepository;
        private final GameweekRepository gameweekRepository;
        private final UserTeamGameweekPointsRepository userTeamGameweekPointsRepository;
        private final UserTeamGameweekLineupRepository userTeamGameweekLineupRepository;
        private final UserTeamGameweekTransfersRepository userTeamGameweekTransfersRepository;
        private final ScoringService scoringService;

        private static final int MAX_SQUAD_SIZE = 15;
        private static final int MAX_PLAYERS_PER_REAL_TEAM = 3;
        private static final BigDecimal TOTAL_BUDGET = BigDecimal.valueOf(100.0);

        // Position limits: 2 GK, 5 DEF, 5 MID, 3 FWD
        private static final Map<Position, Integer> POSITION_LIMITS = Map.of(
                        Position.GK, 2,
                        Position.DEF, 5,
                        Position.MID, 5,
                        Position.FWD, 3);

        @Transactional
        public UserTeamPlayer addPlayerToSquad(Long userId, Long playerId) {
                log.info("Adding player {} to user {} squad", playerId, userId);

                // Get or create user team
                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseGet(() -> createDefaultTeam(userId));

                // Get player
                Player player = playerRepository.findById(playerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Player not found with id: " + playerId));

                // Validation 1: Squad Size (Max 15 players)
                if (userTeam.getTeamPlayers().size() >= MAX_SQUAD_SIZE) {
                        throw new TeamFullException("Squad is full. Maximum " + MAX_SQUAD_SIZE + " players allowed.");
                }

                // Validation 2: Budget Check
                BigDecimal newRemainingBudget = userTeam.getRemainingBudget().subtract(player.getPrice());
                if (newRemainingBudget.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InsufficientBudgetException(
                                        String.format("Insufficient budget. Player costs %.1f, remaining budget: %.1f",
                                                        player.getPrice().doubleValue(),
                                                        userTeam.getRemainingBudget().doubleValue()));
                }

                // Validation 3: Team Limit (Max 3 players from same real team)
                long playersFromSameTeam = userTeam.getTeamPlayers().stream()
                                .filter(tp -> tp.getPlayer().getRealTeam().equals(player.getRealTeam()))
                                .count();

                if (playersFromSameTeam >= MAX_PLAYERS_PER_REAL_TEAM) {
                        throw new InvalidTransferException(
                                        String.format("Cannot have more than %d players from %s",
                                                        MAX_PLAYERS_PER_REAL_TEAM, player.getRealTeam()));
                }

                // Validation 4: Position Balance (2 GK, 5 DEF, 5 MID, 3 FWD)
                Map<Position, Long> currentPositionCounts = userTeam.getTeamPlayers().stream()
                                .collect(Collectors.groupingBy(
                                                tp -> tp.getPlayer().getPosition(),
                                                Collectors.counting()));

                long currentCountForPosition = currentPositionCounts.getOrDefault(player.getPosition(), 0L);
                int maxForPosition = POSITION_LIMITS.get(player.getPosition());

                if (currentCountForPosition >= maxForPosition) {
                        throw new InvalidTransferException(
                                        String.format("Position limit reached. Maximum %d %s allowed.",
                                                        maxForPosition, player.getPosition()));
                }

                // Check for duplicate player
                boolean alreadyInTeam = userTeam.getTeamPlayers().stream()
                                .anyMatch(tp -> tp.getPlayer().getId().equals(playerId));

                if (alreadyInTeam) {
                        throw new InvalidTransferException("Player already in your squad");
                }

                // All validations passed - add player to squad
                UserTeamPlayer userTeamPlayer = UserTeamPlayer.builder()
                                .team(userTeam)
                                .player(player)
                                .purchasePrice(player.getPrice())
                                .build();

                userTeam.getTeamPlayers().add(userTeamPlayer);
                userTeam.setRemainingBudget(newRemainingBudget);

                userTeamRepository.save(userTeam);
                log.info("Successfully added player {} to squad. Remaining budget: {}",
                                player.getName(), newRemainingBudget);

                return userTeamPlayer;
        }

        @Transactional
        public void removePlayerFromSquad(Long userId, Long playerId) {
                log.info("Removing player {} from user {} squad", playerId, userId);

                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));

                UserTeamPlayer playerToRemove = userTeam.getTeamPlayers().stream()
                                .filter(tp -> tp.getPlayer().getId().equals(playerId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("Player not in your squad"));

                // Refund the purchase price
                BigDecimal refund = playerToRemove.getPurchasePrice();
                userTeam.setRemainingBudget(userTeam.getRemainingBudget().add(refund));

                userTeam.getTeamPlayers().remove(playerToRemove);
                userTeamRepository.save(userTeam);

                log.info("Successfully removed player. Refunded: {}", refund);
        }

        @Transactional
        public UserTeam saveFullSquad(Long userId, List<Long> playerIds) {
                log.info("Saving full squad for user {}", userId);

                ensureTransferWindowOpen();

                if (playerIds == null || playerIds.size() != MAX_SQUAD_SIZE) {
                        throw new InvalidTransferException(
                                        "A complete squad must have exactly " + MAX_SQUAD_SIZE + " players.");
                }

                // Get or create user team
                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseGet(() -> createDefaultTeam(userId));

                // Fetch all players
                List<Player> players = playerRepository.findAllById(playerIds);
                if (players.size() != MAX_SQUAD_SIZE) {
                        throw new ResourceNotFoundException(
                                        "One or more players not found. Received: " + players.size());
                }

                // Validation: Budget Check
                BigDecimal totalCost = players.stream()
                                .map(Player::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalCost.compareTo(TOTAL_BUDGET) > 0) {
                        throw new InsufficientBudgetException(
                                        "Total cost exceeds the maximum budget of " + TOTAL_BUDGET);
                }

                // Validation: Team Limit (Max 3 players from same real team)
                Map<String, Long> teamCounts = players.stream()
                                .collect(Collectors.groupingBy(Player::getRealTeam, Collectors.counting()));

                for (Map.Entry<String, Long> entry : teamCounts.entrySet()) {
                        if (entry.getValue() > MAX_PLAYERS_PER_REAL_TEAM) {
                                throw new InvalidTransferException("Cannot have more than " + MAX_PLAYERS_PER_REAL_TEAM
                                                + " players from " + entry.getKey());
                        }
                }

                // Validation: Position Balance
                Map<Position, Long> positionCounts = players.stream()
                                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

                for (Map.Entry<Position, Integer> entry : POSITION_LIMITS.entrySet()) {
                        long count = positionCounts.getOrDefault(entry.getKey(), 0L);
                        if (count > entry.getValue()) {
                                throw new InvalidTransferException(
                                                "Position limit exceeded for " + entry.getKey() + ".");
                        }
                        if (count < entry.getValue()) {
                                throw new InvalidTransferException(
                                                "Not enough players for position " + entry.getKey() + ".");
                        }
                }

                // Atomic update: Clear and replace
                userTeam.getTeamPlayers().clear();

                // Auto-assign starter status: 1 GK, 4 DEF, 4 MID, 2 FWD = 11 starters
                Map<Position, Integer> starterCounts = new java.util.HashMap<>();
                Map<Position, Integer> starterLimits = Map.of(
                                Position.GK, 1, Position.DEF, 4, Position.MID, 4, Position.FWD, 2);

                for (Player player : players) {
                        int currentCount = starterCounts.getOrDefault(player.getPosition(), 0);
                        int limit = starterLimits.getOrDefault(player.getPosition(), 0);
                        boolean isStarter = currentCount < limit;

                        UserTeamPlayer userTeamPlayer = UserTeamPlayer.builder()
                                        .team(userTeam)
                                        .player(player)
                                        .purchasePrice(player.getPrice())
                                        .starter(isStarter)
                                        .build();
                        userTeam.getTeamPlayers().add(userTeamPlayer);

                        starterCounts.put(player.getPosition(), currentCount + 1);
                }

                userTeam.setRemainingBudget(TOTAL_BUDGET.subtract(totalCost));
                log.info("Successfully updated full squad for user {}", userId);

                UserTeam saved = userTeamRepository.save(userTeam);
                persistSnapshotForUpcomingGameweek(saved);
                return saved;
        }

        @Transactional
        public UserTeam saveTransfers(Long userId, List<Long> playerIds, int transferCost) {
                log.info("Saving transfers for user {} with cost {} pts", userId, transferCost);

                ensureTransferWindowOpen();

                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                Integer targetGameweek = status.activeGameweek() != null ? status.activeGameweek() : status.nextGameweek();
                boolean betweenGameweeksDraft = status.activeGameweek() == null && status.nextGameweek() != null;

                // Get the current team before updating
                UserTeam currentTeam = userTeamRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));

                // Preserve current official squad so pre-deadline transfers can be stored as a
                // draft snapshot for the next gameweek without committing immediately.
                List<UserTeamPlayerState> originalTeamState = currentTeam.getTeamPlayers().stream()
                                .map(tp -> new UserTeamPlayerState(tp.getPlayer(), tp.getPurchasePrice(), tp.isStarter()))
                                .toList();
                BigDecimal originalRemainingBudget = currentTeam.getRemainingBudget();
                int originalTotalPoints = currentTeam.getTotalPoints() == null ? 0 : currentTeam.getTotalPoints();

                // Count current transfers (players in new squad not in old squad)
                Set<Long> oldPlayerIds = currentTeam.getTeamPlayers().stream()
                                .map(tp -> tp.getPlayer().getId())
                                .collect(Collectors.toSet());
                
                Set<Long> newPlayerIds = new HashSet<>(playerIds);
                
                int transferCount = 0;
                for (Long playerId : newPlayerIds) {
                        if (!oldPlayerIds.contains(playerId)) {
                                transferCount++;
                        }
                }
                
                log.info("Transfer count calculated: {}", transferCount);

                // Reuse the full squad save logic (validates 15 players, budget, positions,
                // team limits)
                UserTeam updatedTeam = saveFullSquad(userId, playerIds);

                // Always rewrite the target gameweek lineup from the submitted transfer list.
                // This guarantees pre-deadline updates replace players in the GW snapshot table.
                if (targetGameweek != null) {
                        persistSnapshotForGameweekFromPlayerIds(updatedTeam, playerIds, targetGameweek);
                }

                // Record transfer count for the target gameweek (active GW if running,
                // otherwise upcoming GW during pre-deadline period).
                if (transferCount > 0 && targetGameweek != null) {
                                Optional<Gameweek> gameweekOpt = gameweekRepository.findByGameweekNumber(targetGameweek);

                                UserTeamGameweekTransfers transferRecord = userTeamGameweekTransfersRepository
                                        .findByTeamIdAndGameweekNumber(updatedTeam.getId(), targetGameweek)
                                        .orElseGet(() -> UserTeamGameweekTransfers.builder()
                                                .team(updatedTeam)
                                                .gameweek(gameweekOpt.orElse(null))
                                                .gameweekNumber(targetGameweek)
                                                .transferCount(0)
                                                .build());

                                transferRecord.setTransferCount(transferCount);
                                userTeamGameweekTransfersRepository.save(transferRecord);

                                log.info("Recorded {} transfers for gameweek {} for team {}",
                                                transferCount, targetGameweek, updatedTeam.getId());
                }

                // Deduct the transfer cost from totalPoints (cannot go below 0)
                int currentPoints = updatedTeam.getTotalPoints() != null ? updatedTeam.getTotalPoints() : 0;
                int newPoints = Math.max(0, currentPoints - transferCost);
                updatedTeam.setTotalPoints(newPoints);

                log.info("Transfer cost of {} pts deducted. Points updated from {} to {}", transferCost, currentPoints,
                                newPoints);

                if (betweenGameweeksDraft) {
                        restoreOfficialSquad(updatedTeam, originalTeamState, originalRemainingBudget, originalTotalPoints);
                }

                UserTeam saved = userTeamRepository.save(updatedTeam);

                // Sync gameweek points to apply penalty to UserTeamGameweekPoints table
                scoringService.syncTeamGameweekPoints(saved);

                return saved;
        }

        @Transactional(readOnly = true)
        public TeamLineupResponse getTeamLineup(Long userId) {
                UserTeam userTeam = getDetailedTeam(userId);

                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                if (status.activeGameweek() == null && status.nextGameweek() != null) {
                        UserTeamGameweekLineup draftSnapshot = getOrCreateSnapshotForGameweek(userTeam, status.nextGameweek());
                        return toLineupResponseFromSnapshot(userTeam, draftSnapshot);
                }

                return toLineupResponse(userTeam);
        }

        @Transactional
        public TeamLineupResponse makeSubstitution(Long userId, Long starterPlayerId, Long benchPlayerId) {
                ensureTransferWindowOpen();

                if (starterPlayerId == null || benchPlayerId == null) {
                        throw new InvalidTransferException("Both starter and bench players are required.");
                }

                if (starterPlayerId.equals(benchPlayerId)) {
                        throw new InvalidTransferException("Starter and bench player must be different.");
                }

                UserTeam userTeam = getDetailedTeam(userId);
                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();

                if (status.activeGameweek() == null && status.nextGameweek() != null) {
                        UserTeamGameweekLineup draftSnapshot = getOrCreateSnapshotForGameweek(userTeam, status.nextGameweek());

                        UserTeamGameweekLineupPlayer starter = draftSnapshot.getPlayers().stream()
                                        .filter(lp -> lp.getPlayer().getId().equals(starterPlayerId))
                                        .findFirst()
                                        .orElseThrow(() -> new InvalidTransferException("Starter player is not in your squad."));

                        UserTeamGameweekLineupPlayer bench = draftSnapshot.getPlayers().stream()
                                        .filter(lp -> lp.getPlayer().getId().equals(benchPlayerId))
                                        .findFirst()
                                        .orElseThrow(() -> new InvalidTransferException("Bench player is not in your squad."));

                        if (!starter.isStarter()) {
                                throw new InvalidTransferException("Selected starter player is currently on the bench.");
                        }

                        if (bench.isStarter()) {
                                throw new InvalidTransferException("Selected bench player is currently in the starting lineup.");
                        }

                        if (starter.getPlayer().getPosition() != bench.getPlayer().getPosition()) {
                                throw new InvalidTransferException("Substitutions are only allowed between players of the same position.");
                        }

                        starter.setStarter(false);
                        bench.setStarter(true);
                        draftSnapshot.setCapturedAt(LocalDateTime.now());
                        userTeamGameweekLineupRepository.save(draftSnapshot);
                        return toLineupResponseFromSnapshot(userTeam, draftSnapshot);
                }

                UserTeamPlayer starter = userTeam.getTeamPlayers().stream()
                                .filter(tp -> tp.getPlayer().getId().equals(starterPlayerId))
                                .findFirst()
                                .orElseThrow(() -> new InvalidTransferException("Starter player is not in your squad."));

                UserTeamPlayer bench = userTeam.getTeamPlayers().stream()
                                .filter(tp -> tp.getPlayer().getId().equals(benchPlayerId))
                                .findFirst()
                                .orElseThrow(() -> new InvalidTransferException("Bench player is not in your squad."));

                if (!starter.isStarter()) {
                        throw new InvalidTransferException("Selected starter player is currently on the bench.");
                }

                if (bench.isStarter()) {
                        throw new InvalidTransferException("Selected bench player is currently in the starting lineup.");
                }

                if (starter.getPlayer().getPosition() != bench.getPlayer().getPosition()) {
                        throw new InvalidTransferException("Substitutions are only allowed between players of the same position.");
                }

                starter.setStarter(false);
                bench.setStarter(true);

                UserTeam saved = userTeamRepository.save(userTeam);
                persistSnapshotForUpcomingGameweek(saved);
                return toLineupResponse(saved);
        }

        @Transactional
        public void saveLineup(Long userId, List<Long> starterPlayerIds) {
                ensureTransferWindowOpen();

                if (starterPlayerIds == null || starterPlayerIds.size() != 11) {
                        throw new InvalidTransferException("Starting lineup must contain exactly 11 players.");
                }

                Set<Long> uniqueStarterIds = new HashSet<>(starterPlayerIds);
                if (uniqueStarterIds.size() != 11) {
                        throw new InvalidTransferException("Starting lineup contains duplicate players.");
                }

                UserTeam userTeam = getDetailedTeam(userId);
                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();

                final Set<Long> squadPlayerIds;
                final Map<Long, Position> positionByPlayerId;

                if (status.activeGameweek() == null && status.nextGameweek() != null) {
                        UserTeamGameweekLineup draftSnapshot = getOrCreateSnapshotForGameweek(userTeam, status.nextGameweek());
                        if (draftSnapshot.getPlayers().size() != MAX_SQUAD_SIZE) {
                                throw new InvalidTransferException("A complete 15-player squad is required before saving lineup.");
                        }

                        squadPlayerIds = draftSnapshot.getPlayers().stream()
                                        .map(lp -> lp.getPlayer().getId())
                                        .collect(Collectors.toSet());
                        positionByPlayerId = draftSnapshot.getPlayers().stream()
                                        .collect(Collectors.toMap(lp -> lp.getPlayer().getId(), lp -> lp.getPlayer().getPosition()));
                } else {
                        Long teamId = userTeamRepository.findTeamIdByUserId(userId)
                                        .orElseThrow(() -> new ResourceNotFoundException("User team not found"));

                        List<LineupPlayerMeta> lineupMeta = userTeamPlayerRepository.findLineupMetaByTeamId(teamId);
                        if (lineupMeta.size() != MAX_SQUAD_SIZE) {
                                throw new InvalidTransferException("A complete 15-player squad is required before saving lineup.");
                        }

                        squadPlayerIds = lineupMeta.stream().map(LineupPlayerMeta::playerId).collect(Collectors.toSet());
                        positionByPlayerId = lineupMeta.stream().collect(Collectors.toMap(LineupPlayerMeta::playerId, LineupPlayerMeta::position));
                }

                if (!squadPlayerIds.containsAll(uniqueStarterIds)) {
                        throw new InvalidTransferException("One or more selected starters are not in your squad.");
                }

                int gkCount = 0;
                int defCount = 0;
                int fwdCount = 0;

                for (Long playerId : uniqueStarterIds) {
                        Position position = positionByPlayerId.get(playerId);
                        if (position == null) {
                                continue;
                        }
                        if (position == Position.GK) {
                                gkCount++;
                        } else if (position == Position.DEF) {
                                defCount++;
                        } else if (position == Position.FWD) {
                                fwdCount++;
                        }
                }

                if (gkCount != 1) {
                        throw new InvalidTransferException("Starting lineup must include exactly 1 goalkeeper.");
                }

                if (defCount < 3) {
                        throw new InvalidTransferException("Starting lineup must include at least 3 defenders.");
                }

                if (fwdCount < 1) {
                        throw new InvalidTransferException("Starting lineup must include at least 1 attacker.");
                }

                if (status.activeGameweek() == null && status.nextGameweek() != null) {
                        UserTeamGameweekLineup draftSnapshot = getOrCreateSnapshotForGameweek(userTeam, status.nextGameweek());
                        for (UserTeamGameweekLineupPlayer lp : draftSnapshot.getPlayers()) {
                                lp.setStarter(uniqueStarterIds.contains(lp.getPlayer().getId()));
                        }
                        draftSnapshot.setCapturedAt(LocalDateTime.now());
                        userTeamGameweekLineupRepository.save(draftSnapshot);
                        return;
                }

                Long teamId = userTeamRepository.findTeamIdByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));
                userTeamPlayerRepository.clearStartersByTeamId(teamId);
                userTeamPlayerRepository.setStartersByTeamId(teamId, starterPlayerIds);

                persistSnapshotForUpcomingGameweek(userTeam);
        }

        private UserTeam getDetailedTeam(Long userId) {
                return userTeamRepository.findDetailedByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));
        }

        private TeamLineupResponse toLineupResponse(UserTeam userTeam) {
                List<TeamLineupPlayerResponse> players = userTeam.getTeamPlayers().stream()
                                .sorted(Comparator
                                                .comparing(UserTeamPlayer::isStarter).reversed()
                                                .thenComparing(tp -> tp.getPlayer().getPosition().name())
                                                .thenComparing(tp -> tp.getPlayer().getName()))
                                .map(tp -> new TeamLineupPlayerResponse(
                                                tp.getPlayer().getId(),
                                                tp.getPlayer().getName(),
                                                tp.getPlayer().getPosition(),
                                                tp.getPlayer().getRealTeam(),
                                                tp.getPlayer().getPrice(),
                                                tp.getPlayer().getTotalPoints(),
                                                tp.isStarter()))
                                .toList();

                return new TeamLineupResponse(
                                userTeam.getId(),
                                userTeam.getTeamName(),
                                userTeam.getRemainingBudget(),
                                players);
        }

        private TeamLineupResponse toLineupResponseFromSnapshot(UserTeam team, UserTeamGameweekLineup snapshot) {
                List<TeamLineupPlayerResponse> players = snapshot.getPlayers().stream()
                                .sorted(Comparator
                                                .comparing(UserTeamGameweekLineupPlayer::isStarter).reversed()
                                                .thenComparing(lp -> lp.getPlayer().getPosition().name())
                                                .thenComparing(lp -> lp.getPlayer().getName()))
                                .map(lp -> new TeamLineupPlayerResponse(
                                                lp.getPlayer().getId(),
                                                lp.getPlayer().getName(),
                                                lp.getPlayer().getPosition(),
                                                lp.getPlayer().getRealTeam(),
                                                lp.getPlayer().getPrice(),
                                                lp.getPlayer().getTotalPoints(),
                                                lp.isStarter()))
                                .toList();

                return new TeamLineupResponse(
                                team.getId(),
                                team.getTeamName(),
                                team.getRemainingBudget(),
                                players);
        }

        private void ensureTransferWindowOpen() {
                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                if (!status.transfersAllowed()) {
                        if (status.activeGameweek() != null) {
                                throw new InvalidTransferException(
                                                "Transfers are locked. We are currently in Gameweek "
                                                                + status.activeGameweek() + ".");
                        }
                        throw new InvalidTransferException("Transfers are currently locked.");
                }
        }

        @Transactional
        public UserTeam getUserSquad(Long userId) {
                UserTeam team = userTeamRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));
                return applyActiveGameweekSnapshotIfNeeded(team);
        }

        @Transactional(readOnly = true)
        public SquadStatistics getSquadStatistics(Long userId) {
                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseGet(() -> createDefaultTeam(userId));

                Map<Position, Long> positionCounts = userTeam.getTeamPlayers().stream()
                                .collect(Collectors.groupingBy(
                                                tp -> tp.getPlayer().getPosition(),
                                                Collectors.counting()));

                Map<String, Long> teamCounts = userTeam.getTeamPlayers().stream()
                                .collect(Collectors.groupingBy(
                                                tp -> tp.getPlayer().getRealTeam(),
                                                Collectors.counting()));

                return SquadStatistics.builder()
                                .totalPlayers(userTeam.getTeamPlayers().size())
                                .remainingBudget(userTeam.getRemainingBudget())
                                .totalPoints(userTeam.getTotalPoints())
                                .goalkeepers(positionCounts.getOrDefault(Position.GK, 0L).intValue())
                                .defenders(positionCounts.getOrDefault(Position.DEF, 0L).intValue())
                                .midfielders(positionCounts.getOrDefault(Position.MID, 0L).intValue())
                                .forwards(positionCounts.getOrDefault(Position.FWD, 0L).intValue())
                                .teamCounts(teamCounts)
                                .isComplete(userTeam.getTeamPlayers().size() == MAX_SQUAD_SIZE)
                                .build();
        }

        @Transactional
        public GameweekStatsResponse getGameweekStats(Long userId, int gameweek) {
                int maxVisibleLineupGameweek = getMaxVisibleLineupGameweek();
                if (gameweek > maxVisibleLineupGameweek) {
                        return new GameweekStatsResponse(gameweek, 0, 0, List.of());
                }

                scoringService.ensureGameweekStatsInitialized(gameweek);

                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseGet(() -> createDefaultTeam(userId));

                Long gameweekId = gameweekRepository.findByGameweekNumber(gameweek)
                                .map(Gameweek::getId)
                                .orElse(null);

                Optional<Gameweek> requestedGameweekOpt = gameweekRepository.findByGameweekNumber(gameweek);
                if (isUserNotEligibleForGameweek(userTeam, requestedGameweekOpt.orElse(null))) {
                        int globalHighestPoints = userTeamGameweekPointsRepository.findByGameweekNumber(gameweek).stream()
                                        .mapToInt(row -> row.getPoints() == null ? 0 : row.getPoints())
                                        .max()
                                        .orElse(0);
                        return new GameweekStatsResponse(gameweek, 0, globalHighestPoints, List.of());
                }

                final Set<Long> finishedMatchIds;
                final boolean gameweekStarted;
                if (gameweekId != null) {
                        Optional<Gameweek> gwOpt = gameweekRepository.findById(gameweekId);
                        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
                        gameweekStarted = gwOpt.map(gw -> !gw.getStartDate().isAfter(nowUtc)).orElse(false);
                        finishedMatchIds = matchRepository.findByGameweekId(gameweekId).stream()
                                        .filter(m -> Boolean.TRUE.equals(m.getFinished())
                                                        || !m.getKickoffTime().plusMinutes(105).isAfter(nowUtc))
                                        .map(Match::getId)
                                        .collect(Collectors.toSet());
                } else {
                        gameweekStarted = false;
                        finishedMatchIds = Set.of();
                }

                if (userTeam.getId() != null) {
                        java.util.Optional<UserTeamGameweekLineup> snapshotOpt = userTeamGameweekLineupRepository
                                        .findByTeamIdAndGameweekNumber(userTeam.getId(), gameweek);

                        if (snapshotOpt.isPresent()) {
                                UserTeamGameweekLineup snapshot = snapshotOpt.get();
                                List<PlayerGameweekScore> playerScores = snapshot.getPlayers().stream()
                                                .sorted(Comparator
                                                                .comparing(UserTeamGameweekLineupPlayer::isStarter)
                                                                .reversed()
                                                                .thenComparing(lp -> lp.getPlayer().getName()))
                                                .map(lp -> new PlayerGameweekScore(
                                                                lp.getPlayer().getId(),
                                                                lp.getPlayer().getName(),
                                                                lp.getPlayer().getPosition().name(),
                                                                lp.getPlayer().getRealTeam(),
                                                                lp.getPlayer().getPrice().doubleValue(),
                                                                lp.getPlayer().getTotalPoints(),
                                                                gameweekStarted
                                                                                ? calculatePlayerPointsSoFar(
                                                                                                lp.getPlayer().getId(),
                                                                                                gameweek,
                                                                                                gameweekId,
                                                                                                finishedMatchIds)
                                                                                : 0,
                                                                lp.isStarter()))
                                                .toList();

                                int teamPoints = playerScores.stream()
                                                .filter(PlayerGameweekScore::isStarter)
                                                .mapToInt(PlayerGameweekScore::getPoints)
                                                .sum();

                                int transferPenalty = getTransferPenaltyForTeamGameweek(userTeam.getId(), gameweek);
                                teamPoints += transferPenalty;

                                if (userTeam.getId() != null) {
                                        UserTeamGameweekPoints row = userTeamGameweekPointsRepository
                                                        .findByTeamIdAndGameweekNumber(userTeam.getId(), gameweek)
                                                        .orElseGet(() -> UserTeamGameweekPoints.builder()
                                                                        .team(userTeam)
                                                                        .gameweek(gameweekId != null
                                                                                        ? gameweekRepository.findById(gameweekId)
                                                                                                        .orElse(null)
                                                                                        : null)
                                                                        .gameweekNumber(gameweek)
                                                                        .build());
                                        row.setPoints(teamPoints);
                                        userTeamGameweekPointsRepository.save(row);
                                }

                                int globalHighestPoints = userTeamGameweekPointsRepository.findByGameweekNumber(gameweek)
                                                .stream()
                                                .mapToInt(row -> row.getPoints() == null ? 0 : row.getPoints())
                                                .max()
                                                .orElse(teamPoints);

                                return new GameweekStatsResponse(
                                                gameweek,
                                                teamPoints,
                                                globalHighestPoints,
                                                playerScores);
                        }

                        // Once a gameweek has started, lineup must come from its snapshot.
                        // If missing, do not fall back to current squad to avoid historical drift.
                        if (gameweekStarted) {
                                int globalHighestPoints = userTeamGameweekPointsRepository.findByGameweekNumber(gameweek)
                                                .stream()
                                                .mapToInt(row -> row.getPoints() == null ? 0 : row.getPoints())
                                                .max()
                                                .orElse(0);
                                return new GameweekStatsResponse(gameweek, 0, globalHighestPoints, List.of());
                        }
                }

                // Map each player to their score for this gameweek (includes bench)
                List<PlayerGameweekScore> playerScores = userTeam.getTeamPlayers().stream().map(tp -> {
                        int points = calculatePlayerPointsSoFar(tp.getPlayer().getId(), gameweek, gameweekId,
                                        finishedMatchIds);
                        return new PlayerGameweekScore(tp.getPlayer().getId(), tp.getPlayer().getName(),
                                        tp.getPlayer().getPosition().name(), tp.getPlayer().getRealTeam(),
                                        tp.getPlayer().getPrice().doubleValue(), tp.getPlayer().getTotalPoints(), points,
                                        tp.isStarter());
                }).toList();

                // teamPoints = ONLY starters
                int teamPoints = playerScores.stream()
                                .filter(PlayerGameweekScore::isStarter)
                                .mapToInt(PlayerGameweekScore::getPoints)
                                .sum();

                int transferPenalty = getTransferPenaltyForTeamGameweek(userTeam.getId(), gameweek);
                teamPoints += transferPenalty;

                if (userTeam.getId() != null) {
                        UserTeamGameweekPoints row = userTeamGameweekPointsRepository
                                        .findByTeamIdAndGameweekNumber(userTeam.getId(), gameweek)
                                        .orElseGet(() -> UserTeamGameweekPoints.builder()
                                                        .team(userTeam)
                                                        .gameweek(gameweekId != null
                                                                        ? gameweekRepository.findById(gameweekId)
                                                                                        .orElse(null)
                                                                        : null)
                                                        .gameweekNumber(gameweek)
                                                        .build());
                        row.setPoints(teamPoints);
                        userTeamGameweekPointsRepository.save(row);
                }

                // Global highest = ONLY starters for each team
                int globalHighestPoints = userTeamGameweekPointsRepository.findByGameweekNumber(gameweek).stream()
                                .mapToInt(row -> row.getPoints() == null ? 0 : row.getPoints())
                                .max()
                                .orElse(teamPoints);

                return new GameweekStatsResponse(
                                gameweek,
                                teamPoints,
                                globalHighestPoints,
                                playerScores);
        }

        private int getMaxVisibleLineupGameweek() {
                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                if (status.activeGameweek() != null) {
                        return status.activeGameweek();
                }
                if (status.nextGameweek() != null) {
                        return status.nextGameweek();
                }
                return Integer.MAX_VALUE;
        }

        @Transactional(readOnly = true)
        public List<GameweekTotalPointsResponse> getPersistedGameweekTotals(Long userId) {
                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseGet(() -> createDefaultTeam(userId));

                return userTeamGameweekPointsRepository.findByTeamIdOrderByGameweekNumberAsc(userTeam.getId()).stream()
                                .map(row -> new GameweekTotalPointsResponse(
                                                row.getGameweekNumber(),
                                                row.getPoints()))
                                .toList();
        }

        private int calculatePlayerPointsSoFar(Long playerId, int gameweekNumber, Long gameweekId,
                        Set<Long> finishedMatchIds) {
                if (gameweekId == null) {
                        return 0;
                }

                if (finishedMatchIds.isEmpty()) {
                        return 0;
                }

                // Includes negative point events (yellow/red cards) as rows are summed directly.
                List<PlayerGameweekStats> rows = statsRepository.findByPlayerIdAndGameweekId(playerId, gameweekId);

                // Backward compatibility: older rows may have no gameweek relation set.
                if (rows.isEmpty()) {
                        return deduplicatedPoints(statsRepository.findAllByPlayerIdAndGameweekNumber(playerId, gameweekNumber),
                                        finishedMatchIds);
                }

                return deduplicatedPoints(rows, finishedMatchIds);
        }

        private int deduplicatedPoints(List<PlayerGameweekStats> rows, Set<Long> finishedMatchIds) {
                if (rows.isEmpty()) {
                        return 0;
                }

                Map<String, PlayerGameweekStats> byKey = rows.stream()
                                .filter(s -> finishedMatchIds == null
                                                || finishedMatchIds.isEmpty()
                                                || s.getMatch() == null
                                                || finishedMatchIds.contains(s.getMatch().getId()))
                                .sorted((a, b) -> Long.compare(
                                                b.getId() == null ? 0L : b.getId(),
                                                a.getId() == null ? 0L : a.getId()))
                                .collect(Collectors.toMap(
                                                s -> s.getMatch() != null ? "M-" + s.getMatch().getId()
                                                                : "R-" + (s.getId() == null ? 0L : s.getId()),
                                                s -> s,
                                                (existing, ignored) -> existing,
                                                LinkedHashMap::new));

                return byKey.values().stream()
                                .mapToInt(s -> s.getPointsEarned() == null ? 0 : s.getPointsEarned())
                                .sum();
        }

        private int getTransferPenaltyForTeamGameweek(Long teamId, int gameweek) {
                if (teamId == null) {
                        return 0;
                }

                int transferCount = userTeamGameweekTransfersRepository
                                .findByTeamIdAndGameweekNumber(teamId, gameweek)
                                .map(UserTeamGameweekTransfers::getTransferCount)
                                .orElse(0);

                if (transferCount <= 1) {
                        return 0;
                }

                return -(transferCount - 1) * 4;
        }

        private boolean isUserNotEligibleForGameweek(UserTeam team, Gameweek gameweek) {
                if (team == null || gameweek == null || team.getUser() == null || team.getUser().getCreatedAt() == null) {
                        return false;
                }

                // A user can only participate in gameweeks that start on/after their registration time.
                return team.getUser().getCreatedAt().isAfter(gameweek.getStartDate());
        }

        private UserTeamGameweekLineup getOrCreateSnapshotForGameweek(UserTeam team, int gameweekNumber) {
                Optional<UserTeamGameweekLineup> existing = userTeamGameweekLineupRepository
                                .findByTeamIdAndGameweekNumber(team.getId(), gameweekNumber);
                if (existing.isPresent()) {
                        return existing.get();
                }

                Gameweek gameweek = gameweekRepository.findByGameweekNumber(gameweekNumber).orElse(null);
                UserTeamGameweekLineup snapshot = UserTeamGameweekLineup.builder()
                                .team(team)
                                .gameweek(gameweek)
                                .gameweekNumber(gameweekNumber)
                                .capturedAt(LocalDateTime.now())
                                .build();

                int teamPoints = 0;
                for (UserTeamPlayer tp : team.getTeamPlayers()) {
                        int points = deduplicatedPoints(
                                        statsRepository.findAllByPlayerIdAndGameweekNumber(tp.getPlayer().getId(), gameweekNumber),
                                        java.util.Set.of());

                        UserTeamGameweekLineupPlayer row = UserTeamGameweekLineupPlayer.builder()
                                        .lineup(snapshot)
                                        .player(tp.getPlayer())
                                        .starter(tp.isStarter())
                                        .pointsEarned(points)
                                        .build();
                        snapshot.getPlayers().add(row);
                        if (tp.isStarter()) {
                                teamPoints += points;
                        }
                }

                snapshot.setTeamPoints(teamPoints);
                return userTeamGameweekLineupRepository.save(snapshot);
        }

        private void restoreOfficialSquad(UserTeam team, List<UserTeamPlayerState> originalTeamState,
                        BigDecimal originalRemainingBudget, int originalTotalPoints) {
                team.getTeamPlayers().clear();

                for (UserTeamPlayerState state : originalTeamState) {
                        UserTeamPlayer restored = UserTeamPlayer.builder()
                                        .team(team)
                                        .player(state.player())
                                        .purchasePrice(state.purchasePrice())
                                        .starter(state.starter())
                                        .build();
                        team.getTeamPlayers().add(restored);
                }

                team.setRemainingBudget(originalRemainingBudget);
                team.setTotalPoints(originalTotalPoints);
        }

        private UserTeam applyActiveGameweekSnapshotIfNeeded(UserTeam team) {
                if (team.getId() == null) {
                        return team;
                }

                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                Integer activeGameweek = status.activeGameweek();
                if (activeGameweek == null) {
                        return team;
                }

                Optional<UserTeamGameweekLineup> snapshotOpt = userTeamGameweekLineupRepository
                                .findByTeamIdAndGameweekNumber(team.getId(), activeGameweek);
                if (snapshotOpt.isEmpty()) {
                        return team;
                }

                UserTeamGameweekLineup snapshot = snapshotOpt.get();
                Set<Long> currentPlayerIds = team.getTeamPlayers().stream()
                                .map(tp -> tp.getPlayer().getId())
                                .collect(Collectors.toSet());
                Set<Long> snapshotPlayerIds = snapshot.getPlayers().stream()
                                .map(lp -> lp.getPlayer().getId())
                                .collect(Collectors.toSet());

                if (currentPlayerIds.equals(snapshotPlayerIds)) {
                        return team;
                }

                team.getTeamPlayers().clear();
                BigDecimal spent = BigDecimal.ZERO;
                for (UserTeamGameweekLineupPlayer lp : snapshot.getPlayers()) {
                        BigDecimal purchasePrice = lp.getPlayer().getPrice();
                        UserTeamPlayer row = UserTeamPlayer.builder()
                                        .team(team)
                                        .player(lp.getPlayer())
                                        .purchasePrice(purchasePrice)
                                        .starter(lp.isStarter())
                                        .build();
                        spent = spent.add(purchasePrice);
                        team.getTeamPlayers().add(row);
                }

                team.setRemainingBudget(TOTAL_BUDGET.subtract(spent));
                return userTeamRepository.save(team);
        }

        private void persistSnapshotForUpcomingGameweek(UserTeam team) {
                if (team.getId() == null) {
                        return;
                }

                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                Integer targetGameweek = status.nextGameweek() != null ? status.nextGameweek() : status.activeGameweek();
                if (targetGameweek == null) {
                        return;
                }

                Gameweek gameweek = gameweekRepository.findByGameweekNumber(targetGameweek).orElse(null);

                UserTeamGameweekLineup snapshot = userTeamGameweekLineupRepository
                                .findByTeamIdAndGameweekNumber(team.getId(), targetGameweek)
                                .orElseGet(() -> UserTeamGameweekLineup.builder()
                                                .team(team)
                                                .gameweek(gameweek)
                                                .gameweekNumber(targetGameweek)
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

                Map<Long, UserTeamPlayer> uniquePlayersById = new HashMap<>();
                for (UserTeamPlayer tp : team.getTeamPlayers()) {
                        uniquePlayersById.putIfAbsent(tp.getPlayer().getId(), tp);
                }

                int teamPoints = 0;
                for (UserTeamPlayer tp : uniquePlayersById.values()) {
                        int points = deduplicatedPoints(
                                        statsRepository.findAllByPlayerIdAndGameweekNumber(tp.getPlayer().getId(), targetGameweek),
                                        java.util.Set.of());

                        UserTeamGameweekLineupPlayer row = UserTeamGameweekLineupPlayer.builder()
                                        .lineup(snapshot)
                                        .player(tp.getPlayer())
                                        .starter(tp.isStarter())
                                        .pointsEarned(points)
                                        .build();

                        snapshot.getPlayers().add(row);
                        if (tp.isStarter()) {
                                teamPoints += points;
                        }
                }

                snapshot.setTeamPoints(teamPoints);
                userTeamGameweekLineupRepository.save(snapshot);
        }

        private void persistSnapshotForGameweekFromPlayerIds(UserTeam team, List<Long> playerIds, int gameweekNumber) {
                if (team.getId() == null || playerIds == null || playerIds.size() != MAX_SQUAD_SIZE) {
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

                List<Player> players = playerRepository.findAllById(playerIds);
                Map<Long, Player> byId = players.stream().collect(Collectors.toMap(Player::getId, p -> p));

                Map<Position, Integer> starterCounts = new HashMap<>();
                Map<Position, Integer> starterLimits = Map.of(
                                Position.GK, 1,
                                Position.DEF, 4,
                                Position.MID, 4,
                                Position.FWD, 2);

                int teamPoints = 0;
                for (Long playerId : playerIds) {
                        Player player = byId.get(playerId);
                        if (player == null) {
                                continue;
                        }

                        int currentCount = starterCounts.getOrDefault(player.getPosition(), 0);
                        boolean starter = currentCount < starterLimits.getOrDefault(player.getPosition(), 0);
                        starterCounts.put(player.getPosition(), currentCount + 1);

                        int points = deduplicatedPoints(
                                        statsRepository.findAllByPlayerIdAndGameweekNumber(player.getId(), gameweekNumber),
                                        java.util.Set.of());

                        UserTeamGameweekLineupPlayer row = UserTeamGameweekLineupPlayer.builder()
                                        .lineup(snapshot)
                                        .player(player)
                                        .starter(starter)
                                        .pointsEarned(points)
                                        .build();
                        snapshot.getPlayers().add(row);
                        if (starter) {
                                teamPoints += points;
                        }
                }

                snapshot.setTeamPoints(teamPoints);
                userTeamGameweekLineupRepository.save(snapshot);
        }

        @Transactional(readOnly = true)
        public com.example.backend.dto.game.GameweekTransferCountResponse getCurrentGameweekTransferCount(Long userId) {
                UserTeam userTeam = userTeamRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));

                TransferWindowService.TransferWindowStatus status = transferWindowService.getTransferWindowStatus();
                Integer currentGameweek = status.activeGameweek() != null
                                ? status.activeGameweek()
                                : status.nextGameweek();

                if (currentGameweek == null) {
                        return com.example.backend.dto.game.GameweekTransferCountResponse.builder()
                                .gameweek(0)
                                .transferCount(0)
                                .build();
                }

                int transferCount = userTeamGameweekTransfersRepository
                        .findByTeamIdAndGameweekNumber(userTeam.getId(), currentGameweek)
                        .map(com.example.backend.model.entity.UserTeamGameweekTransfers::getTransferCount)
                        .orElse(0);

                return com.example.backend.dto.game.GameweekTransferCountResponse.builder()
                        .gameweek(currentGameweek)
                        .transferCount(transferCount)
                        .build();
        }

        private UserTeam createDefaultTeam(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                UserTeam userTeam = UserTeam.builder()
                                .user(user)
                                .teamName(user.getUsername() + "'s Team")
                                .budget(TOTAL_BUDGET)
                                .remainingBudget(TOTAL_BUDGET)
                                .totalPoints(0)
                                .build();

                return userTeamRepository.save(userTeam);
        }

        private record UserTeamPlayerState(Player player, BigDecimal purchasePrice, boolean starter) {
        }

        @lombok.Data
        @lombok.Builder
        public static class SquadStatistics {
                private int totalPlayers;
                private BigDecimal remainingBudget;
                private int totalPoints;
                private int goalkeepers;
                private int defenders;
                private int midfielders;
                private int forwards;
                private Map<String, Long> teamCounts;
                private boolean isComplete;
        }
}
