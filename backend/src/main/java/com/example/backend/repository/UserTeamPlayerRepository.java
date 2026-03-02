package com.example.backend.repository;

import com.example.backend.model.UserTeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTeamPlayerRepository extends JpaRepository<UserTeamPlayer, Long> {

    List<UserTeamPlayer> findByTeamId(Long teamId);

    List<UserTeamPlayer> findByPlayerId(Long playerId);

    boolean existsByTeamIdAndPlayerId(Long teamId, Long playerId);

    long countByTeamId(Long teamId);
}
