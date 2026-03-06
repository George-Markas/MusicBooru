package com.example.musicbooru.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

public record ErrorResponse(
        HttpStatusCode statusCode,
        String message,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime timestamp,

        String path
) {
    public ErrorResponse(HttpStatusCode statusCode, String message, String path) {
        this(statusCode, message, LocalDateTime.now(), path);
    }
}