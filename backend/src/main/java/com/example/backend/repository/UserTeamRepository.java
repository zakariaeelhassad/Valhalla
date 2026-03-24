package com.example.backend.repository;

import com.example.backend.model.entity.UserTeam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    Optional<UserTeam> findByUserId(Long userId);

    @EntityGraph(attributePaths = { "teamPlayers", "teamPlayers.player" })
    Optional<UserTeam> findDetailedByUserId(Long userId);

    @Query("select t.id from UserTeam t where t.user.id = :userId")
    Optional<Long> findTeamIdByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
