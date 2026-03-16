package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_gameweek_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameweekStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "gameweek_id")
    private Gameweek gameweek;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(nullable = false)
    private Integer gameweekNumber;

    @Column(nullable = false)
    @Builder.Default
    private Integer minutesPlayed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer goals = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer assists = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer yellowCards = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer redCards = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer pointsEarned = 0;

    public void calculatePoints() {
        int points = 0;

        // Goals (position-dependent)
        if (goals > 0 && player != null) {
            points += goals * player.getGoalPoints();
        }

        // Assists
        points += assists * 3;

        // Yellow cards
        points -= yellowCards * 1;

        // Red cards
        points -= redCards * 3;

        this.pointsEarned = points;
    }
}
