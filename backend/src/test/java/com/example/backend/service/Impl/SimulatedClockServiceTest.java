package com.example.backend.service.Impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedClockServiceTest {

    private final SimulatedClockServiceImpl simulatedClockService = new SimulatedClockServiceImpl();

    @Test
    void computeStatus_shouldReturnScheduled_whenKickoffIsInFuture() {
        LocalDateTime simulatedNow = simulatedClockService.getSimulatedNow();
        LocalDateTime kickoff = simulatedNow.plusHours(3);

        String status = simulatedClockService.computeStatus(kickoff, false);

        assertEquals("SCHEDULED", status);
    }

    @Test
    void computeStatus_shouldReturnLive_whenKickoffIsRecentAndNotFinished() {
        LocalDateTime simulatedNow = simulatedClockService.getSimulatedNow();
        LocalDateTime kickoff = simulatedNow.minusMinutes(20);

        String status = simulatedClockService.computeStatus(kickoff, false);

        assertEquals("LIVE", status);
    }

    @Test
    void computeStatus_shouldReturnFinished_whenExplicitlyFinished() {
        LocalDateTime simulatedNow = simulatedClockService.getSimulatedNow();
        LocalDateTime kickoff = simulatedNow.minusMinutes(10);

        String status = simulatedClockService.computeStatus(kickoff, true);

        assertEquals("FINISHED", status);
    }

    @Test
    void getElapsedMinutes_shouldCapAtNinety() {
        LocalDateTime simulatedNow = simulatedClockService.getSimulatedNow();
        LocalDateTime kickoff = simulatedNow.minusMinutes(200);

        int elapsed = simulatedClockService.getElapsedMinutes(kickoff);

        assertEquals(90, elapsed);
    }
}
