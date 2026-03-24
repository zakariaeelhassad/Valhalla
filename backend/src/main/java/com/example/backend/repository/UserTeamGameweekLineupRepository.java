package com.example.backend.repository;

import com.example.backend.model.UserTeamGameweekLineup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTeamGameweekLineupRepository extends JpaRepository<UserTeamGameweekLineup, Long> {

    @EntityGraph(attributePaths = { "players", "players.player" })
    Optional<UserTeamGameweekLineup> findByTeamIdAndGameweekNumber(Long teamId, int gameweekNumber);
}
