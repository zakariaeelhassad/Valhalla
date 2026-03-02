package com.example.backend.repository;

import com.example.backend.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByGameweekId(Long gameweekId);

    List<Match> findByHomeTeamOrAwayTeam(String homeTeam, String awayTeam);

    List<Match> findByFinished(Boolean finished);
}
