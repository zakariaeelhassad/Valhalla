package com.example.backend.repository;

import com.example.backend.dto.team.LineupPlayerMeta;
import com.example.backend.model.entity.UserTeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTeamPlayerRepository extends JpaRepository<UserTeamPlayer, Long> {

    List<UserTeamPlayer> findByTeamId(Long teamId);

    List<UserTeamPlayer> findByPlayerId(Long playerId);

    boolean existsByTeamIdAndPlayerId(Long teamId, Long playerId);

    long countByTeamId(Long teamId);

    @Query("select new com.example.backend.dto.team.LineupPlayerMeta(tp.player.id, tp.player.position) " +
            "from UserTeamPlayer tp where tp.team.id = :teamId")
    List<LineupPlayerMeta> findLineupMetaByTeamId(Long teamId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserTeamPlayer tp set tp.starter = false where tp.team.id = :teamId")
    int clearStartersByTeamId(Long teamId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserTeamPlayer tp set tp.starter = true where tp.team.id = :teamId and tp.player.id in :playerIds")
    int setStartersByTeamId(Long teamId, List<Long> playerIds);
}
