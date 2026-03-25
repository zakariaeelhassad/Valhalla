package com.example.backend.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentaryService implements com.example.backend.service.CommentaryService {

    public String generateCommentary(String playerName, String eventType, int minute) {
        log.info("Generating commentary for: {} - {} at minute {}", playerName, eventType, minute);

        String commentary = generateFallbackCommentary(playerName, eventType, minute);
        log.info("Generated commentary: {}", commentary);

        return commentary;
    }

    private String generateFallbackCommentary(String playerName, String eventType, int minute) {
        // Moroccan Darija/Arabic commentary
        return switch (eventType.toUpperCase()) {
            case "GOAL" -> String.format("⚽ Wow! %s scored in minute %d! What a goal! 🔥", playerName, minute);
            case "ASSIST" -> String.format("💪 Incredible! %s made a key assist in the 90th minute! Well done!",
                    playerName, minute);
            case "RED_CARD" ->
                String.format("😱 Oh no! %s was sent off in the %d minute! Red card!", playerName, minute);
            case "YELLOW_CARD" -> String.format("⚠️ %s received a yellow card in minute %d. Be careful!", playerName, minute);
            default -> String.format("⚽ %s in minute %d!", playerName, minute);
        };
    }
}


