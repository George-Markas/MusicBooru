package org.example.musicbooru.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

public record ErrorResponse(
        HttpStatusCode status,
        String message,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime timestamp,

        String uri
) {
    public ErrorResponse(HttpStatusCode statusCode, String message, String uri) {
        this(statusCode, message, LocalDateTime.now(), uri);
    }
}
