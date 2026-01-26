package com.example.musicbooru.auth;

import org.springframework.http.HttpStatusCode;

public record AuthenticationResponse(
        String cookieString,
        HttpStatusCode statusCode,
        String responseMessage
) {
}