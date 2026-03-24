package com.example.backend.repository;

import com.example.backend.model.UserTeamGameweekTransfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTeamGameweekTransfersRepository extends JpaRepository<UserTeamGameweekTransfers, Long> {
    List<UserTeamGameweekTransfers> findByTeamId(Long teamId);
    Optional<UserTeamGameweekTransfers> findByTeamIdAndGameweekNumber(Long teamId, Integer gameweekNumber);
}
