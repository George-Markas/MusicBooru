package com.example.musicbooru.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String message,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime timestamp,

        String path
) {
    public ErrorResponse(int status, String message, String path) {
        this(status, message, LocalDateTime.now(), path);
    }
}