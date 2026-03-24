package com.example.backend.dto.auth;

import com.example.backend.dto.profile.UserResponse;

public record AuthResponse(
        String token,
        String type,
        UserResponse user) {
    public AuthResponse(String token, UserResponse user) {
        this(token, "Bearer", user);
    }
}
