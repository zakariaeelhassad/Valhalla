package com.example.backend.service;

import java.time.LocalDateTime;

public interface SimulatedClockService {

    LocalDateTime getSimulatedNow();

    String computeStatus(LocalDateTime kickoffTime, boolean finished);

    int getElapsedMinutes(LocalDateTime kickoffTime);
}
