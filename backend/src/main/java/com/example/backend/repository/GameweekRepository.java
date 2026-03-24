package com.example.backend.repository;

import com.example.backend.model.entity.Gameweek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameweekRepository extends JpaRepository<Gameweek, Long> {

    Optional<Gameweek> findByGameweekNumber(Integer gameweekNumber);

    Optional<Gameweek> findByStatus(String status);

    boolean existsByGameweekNumber(Integer gameweekNumber);
}
