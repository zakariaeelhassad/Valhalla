package com.example.backend.service.Impl;

import com.example.backend.dto.game.TransferWindowStatusResponse;
import com.example.backend.model.entity.Gameweek;
import com.example.backend.repository.GameweekRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferWindowServiceTest {

    @Mock
    private GameweekRepository gameweekRepository;

    @InjectMocks
    private TransferWindowService transferWindowService;

    @Test
    void getTransferWindowStatus_shouldReturnNoGameweeks_whenRepositoryEmpty() {
        when(gameweekRepository.findAll()).thenReturn(List.of());

        TransferWindowStatusResponse status = transferWindowService.getTransferWindowStatus();

        assertEquals("NO_GAMEWEEKS", status.phase());
        assertFalse(status.transfersAllowed());
        assertNotNull(status.message());
    }

    @Test
    void getTransferWindowStatus_shouldReturnActive_whenNowInsideGameweek() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Gameweek gw1 = Gameweek.builder()
                .gameweekNumber(1)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .build();
        Gameweek gw2 = Gameweek.builder()
                .gameweekNumber(2)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(8))
                .build();

        when(gameweekRepository.findAll()).thenReturn(List.of(gw1, gw2));

        TransferWindowStatusResponse status = transferWindowService.getTransferWindowStatus();

        assertEquals("ACTIVE", status.phase());
        assertFalse(status.transfersAllowed());
        assertEquals(1, status.activeGameweek());
        assertEquals(2, status.nextGameweek());
    }

    @Test
    void getTransferWindowStatus_shouldReturnBetweenGameweeks_whenNoActiveAndFutureExists() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Gameweek gw1 = Gameweek.builder()
                .gameweekNumber(1)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(3))
                .build();
        Gameweek gw2 = Gameweek.builder()
                .gameweekNumber(2)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(8))
                .build();

        when(gameweekRepository.findAll()).thenReturn(List.of(gw1, gw2));

        TransferWindowStatusResponse status = transferWindowService.getTransferWindowStatus();

        assertEquals("BETWEEN_GAMEWEEKS", status.phase());
        assertTrue(status.transfersAllowed());
        assertEquals(2, status.nextGameweek());
    }
}
