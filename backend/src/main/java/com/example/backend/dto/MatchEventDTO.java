package com.example.backend.dto;

public record MatchEventDTO(
        String type,
        String player,
        String team,
        int minute) {
}
