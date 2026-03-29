package com.example.backend.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentaryServiceTest {

    private final CommentaryServiceImpl commentaryService = new CommentaryServiceImpl();

    @Test
    void generateCommentary_shouldReturnGoalTemplate_forGoalEvent() {
        String text = commentaryService.generateCommentary("Saka", "GOAL", 34);

        assertTrue(text.contains("Saka"));
        assertTrue(text.contains("34"));
        assertTrue(text.contains("goal") || text.contains("GOAL"));
    }

    @Test
    void generateCommentary_shouldReturnRedCardTemplate_forRedCardEvent() {
        String text = commentaryService.generateCommentary("Rodri", "RED_CARD", 71);

        assertTrue(text.contains("Rodri"));
        assertTrue(text.contains("71"));
        assertTrue(text.toUpperCase().contains("RED CARD") || text.toUpperCase().contains("SENT OFF"));
    }

    @Test
    void generateCommentary_shouldReturnDefaultTemplate_forUnknownEvent() {
        String text = commentaryService.generateCommentary("Palmer", "WHATEVER", 12);

        assertTrue(text.contains("Palmer"));
        assertTrue(text.contains("12"));
    }
}
