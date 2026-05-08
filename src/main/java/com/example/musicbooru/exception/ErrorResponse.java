package com.example.musicbooru.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(
        HttpStatus status,
        String message,
        Instant timestamp,
        String path
) {
    public ErrorResponse(HttpStatus status, String message, String path) {
        this(status, message, Instant.now(), path);
    }
}
