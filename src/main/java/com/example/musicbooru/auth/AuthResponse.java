package com.example.musicbooru.auth;

import org.springframework.http.HttpStatus;

public record AuthResponse(
        HttpStatus status,
        String message
) {
}
