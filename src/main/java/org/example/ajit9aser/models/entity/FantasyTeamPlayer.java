package org.example.ajit9aser.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fantasy_team_players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FantasyTeamPlayer {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "fantasy_team_id")
    private FantasyTeam fantasyTeam;


    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;


    private Boolean isStarter;
    private Boolean isCaptain;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}