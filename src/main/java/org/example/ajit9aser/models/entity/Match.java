package org.example.ajit9aser.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "game_week_id")
    private GameWeek gameWeek;


    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private PremierLeagueTeam homeTeam;


    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private PremierLeagueTeam awayTeam;


    private Integer homeScore;
    private Integer awayScore;
    private LocalDateTime matchDate;


    @OneToMany(mappedBy = "match")
    private List<PlayerMatchStats> playerStats;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}