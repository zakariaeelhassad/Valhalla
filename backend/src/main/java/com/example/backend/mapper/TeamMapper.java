package com.example.backend.mapper;

import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.dto.team.TeamResponse;
import com.example.backend.model.entity.Player;
import com.example.backend.model.entity.UserTeam;
import com.example.backend.model.entity.UserTeamPlayer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = PlayerMapper.class)
public interface TeamMapper {

    @Mapping(target = "players", expression = "java(toPlayerSummaries(team.getTeamPlayers()))")
    @Mapping(target = "playerCount", expression = "java(team.getTeamPlayers() == null ? 0 : team.getTeamPlayers().size())")
    TeamResponse toResponse(UserTeam team);

    default List<PlayerSummary> toPlayerSummaries(List<UserTeamPlayer> teamPlayers) {
        if (teamPlayers == null) {
            return List.of();
        }

        List<PlayerSummary> result = new ArrayList<>(teamPlayers.size());
        for (UserTeamPlayer teamPlayer : teamPlayers) {
            result.add(toSummary(teamPlayer));
        }
        return result;
    }

    default PlayerSummary toSummary(UserTeamPlayer teamPlayer) {
        return toSummary(teamPlayer.getPlayer());
    }

    PlayerSummary toSummary(Player player);
}
