package com.example.backend.service;

import com.example.backend.dto.match.MatchEventDTO;

import java.util.List;

public interface MatchEventCacheService {

    void load();

    List<MatchEventDTO> getEvents(String homeTeam, String awayTeam, int gameweek);
}
