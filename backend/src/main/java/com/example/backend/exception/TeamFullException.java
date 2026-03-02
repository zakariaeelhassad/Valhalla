package com.example.backend.exception;

public class TeamFullException extends RuntimeException {

    public TeamFullException(String message) {
        super(message);
    }

    public TeamFullException() {
        super("Team is full. Maximum 15 players allowed.");
    }

    public TeamFullException(int currentSize, int maxSize) {
        super(String.format("Team is full. Current size: %d, Maximum: %d", currentSize, maxSize));
    }
}
