package com.example.backend.repository;

import com.example.backend.model.Player;
import com.example.backend.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByPosition(Position position);

    List<Player> findByRealTeam(String realTeam);

    List<Player> findByPositionAndRealTeam(Position position, String realTeam);

    List<Player> findByNameContainingIgnoreCase(String name);
}
