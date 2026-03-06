package com.example.musicbooru.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.HttpStatusCode;

public record AuthenticationResponse(
        @JsonIgnore
        String cookieString,

        HttpStatusCode status,
        String message
) {
}