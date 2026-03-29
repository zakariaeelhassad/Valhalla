package com.example.backend.service.Impl;

import com.example.backend.dto.player.PlayerResponse;
import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.PlayerMapper;
import com.example.backend.model.entity.Player;
import com.example.backend.model.enums.Position;
import com.example.backend.repository.PlayerRepository;
import com.example.backend.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
        private final PlayerMapper playerMapper;

    @Override
    public Page<PlayerSummary> getPlayers(
            Position position,
            String team,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        List<Player> all = playerRepository.findAll();

        List<Player> filtered = all.stream()
                .filter(p -> position == null || p.getPosition() == position)
                .filter(p -> team == null || p.getRealTeam().equalsIgnoreCase(team))
                .filter(p -> search == null || p.getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<PlayerSummary> pageContent = filtered.subList(start > filtered.size() ? filtered.size() : start, end)
                .stream()
                .map(playerMapper::toSummary)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public PlayerResponse getPlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
                return playerMapper.toResponse(player);
    }

    @Override
    public List<PlayerSummary> searchPlayers(String q, int limit) {
        return playerRepository.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(q.toLowerCase()))
                .limit(limit)
                .map(playerMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTeams() {
        return playerRepository.findAll().stream()
                .map(Player::getRealTeam)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

}
