package com.example.musicbooru.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

public record AuthResponse(
        @JsonIgnore
        String cookie,

        HttpStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String message
) {
}
