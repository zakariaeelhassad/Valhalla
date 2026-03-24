package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_team_gameweek_lineup_players", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "lineup_id", "player_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTeamGameweekLineupPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lineup_id", nullable = false)
    private UserTeamGameweekLineup lineup;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    @Builder.Default
    private boolean starter = false;

    @Column(name = "points_earned", nullable = false)
    @Builder.Default
    private Integer pointsEarned = 0;
}
