package com.example.backend.repository;

import com.example.backend.model.entity.UserTeamGameweekPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTeamGameweekPointsRepository extends JpaRepository<UserTeamGameweekPoints, Long> {

    Optional<UserTeamGameweekPoints> findByTeamIdAndGameweekNumber(Long teamId, int gameweekNumber);

    List<UserTeamGameweekPoints> findByTeamId(Long teamId);

    List<UserTeamGameweekPoints> findByGameweekNumber(int gameweekNumber);

    List<UserTeamGameweekPoints> findByTeamIdOrderByGameweekNumberAsc(Long teamId);
}
