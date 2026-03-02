package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String teamName;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal budget = BigDecimal.valueOf(100.0);

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal remainingBudget = BigDecimal.valueOf(100.0);

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserTeamPlayer> teamPlayers = new ArrayList<>();

    /**
     * Business Rule: Squad must have exactly 15 players
     */
    public boolean isValidSquadSize() {
        return teamPlayers.size() == 15;
    }

    /**
     * Business Rule: Total cost must not exceed budget (100.0)
     */
    public boolean isWithinBudget() {
        return remainingBudget.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Calculate total squad cost
     */
    public BigDecimal calculateTotalCost() {
        return teamPlayers.stream()
                .map(UserTeamPlayer::getPurchasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
