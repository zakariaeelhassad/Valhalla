package com.example.backend.service.Impl;

import com.example.backend.dto.player.PlayerResponse;
import com.example.backend.dto.player.PlayerSummary;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mapper.PlayerMapper;
import com.example.backend.model.entity.Player;
import com.example.backend.model.enums.Position;
import com.example.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerServiceImpl playerService;

    @Test
    void getPlayers_shouldFilterAndPaginate() {
        Player p1 = Player.builder().id(1L).name("Erling Haaland").position(Position.FWD).realTeam("Manchester City").price(BigDecimal.valueOf(14.0)).totalPoints(200).build();
        Player p2 = Player.builder().id(2L).name("Bukayo Saka").position(Position.MID).realTeam("Arsenal").price(BigDecimal.valueOf(10.0)).totalPoints(180).build();
        Player p3 = Player.builder().id(3L).name("Phil Foden").position(Position.MID).realTeam("Manchester City").price(BigDecimal.valueOf(9.0)).totalPoints(170).build();

        when(playerRepository.findAll()).thenReturn(List.of(p1, p2, p3));
        when(playerMapper.toSummary(any(Player.class))).thenAnswer(invocation -> {
            Player p = invocation.getArgument(0);
            return new PlayerSummary(p.getId(), p.getName(), p.getPosition(), p.getRealTeam(), p.getPrice(), p.getTotalPoints());
        });

        Page<PlayerSummary> page = playerService.getPlayers(Position.MID, "Manchester City", "foden", 0, 20, "totalPoints", "desc");

        assertEquals(1, page.getTotalElements());
        assertEquals("Phil Foden", page.getContent().get(0).name());
    }

    @Test
    void getPlayer_shouldReturnMappedPlayer() {
        Player player = Player.builder().id(5L).name("Rodri").position(Position.MID).realTeam("Manchester City").price(BigDecimal.valueOf(8.0)).totalPoints(160).build();
        PlayerResponse mapped = new PlayerResponse(5L, "Rodri", Position.MID, "Manchester City", BigDecimal.valueOf(8.0), 160, 5);

        when(playerRepository.findById(5L)).thenReturn(Optional.of(player));
        when(playerMapper.toResponse(player)).thenReturn(mapped);

        PlayerResponse result = playerService.getPlayer(5L);

        assertEquals("Rodri", result.name());
        assertEquals(5L, result.id());
    }

    @Test
    void getPlayer_shouldThrow_whenNotFound() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> playerService.getPlayer(99L));
    }

    @Test
    void getTeams_shouldReturnDistinctSortedTeams() {
        Player p1 = Player.builder().id(1L).name("A").position(Position.DEF).realTeam("Chelsea").price(BigDecimal.valueOf(5.0)).totalPoints(10).build();
        Player p2 = Player.builder().id(2L).name("B").position(Position.DEF).realTeam("Arsenal").price(BigDecimal.valueOf(5.0)).totalPoints(10).build();
        Player p3 = Player.builder().id(3L).name("C").position(Position.DEF).realTeam("Chelsea").price(BigDecimal.valueOf(5.0)).totalPoints(10).build();

        when(playerRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        List<String> teams = playerService.getTeams();

        assertEquals(List.of("Arsenal", "Chelsea"), teams);
    }
}
