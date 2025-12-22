package org.example.ajit9aser.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ajit9aser.models.enums.PlayerPosition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;


    @Enumerated(EnumType.STRING)
    private PlayerPosition position;


    private BigDecimal price;
    private Boolean active;


    @ManyToOne
    @JoinColumn(name = "premier_league_team_id")
    private PremierLeagueTeam premierLeagueTeam;


    @OneToMany(mappedBy = "player")
    private List<PlayerMatchStats> stats;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}