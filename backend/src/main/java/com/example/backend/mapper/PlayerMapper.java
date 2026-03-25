package com.example.backend.mapper;

import com.example.backend.dto.player.PlayerResponse;
import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.model.entity.Player;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    PlayerSummary toSummary(Player player);

    PlayerResponse toResponse(Player player);

    List<PlayerSummary> toSummaryList(List<Player> players);
}
