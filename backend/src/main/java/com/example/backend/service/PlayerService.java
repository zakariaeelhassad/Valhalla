package com.example.backend.service;

import com.example.backend.dto.player.PlayerResponse;
import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.model.enums.Position;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PlayerService {

    Page<PlayerSummary> getPlayers(
            Position position,
            String team,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir);

    PlayerResponse getPlayer(Long id);

    List<PlayerSummary> searchPlayers(String q, int limit);

    List<String> getTeams();
}
