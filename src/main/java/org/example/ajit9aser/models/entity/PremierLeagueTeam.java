package org.example.ajit9aser.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "premier_league_teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremierLeagueTeam {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;
    private String logo;
    private String city;


    @OneToMany(mappedBy = "premierLeagueTeam")
    private List<Player> players;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
