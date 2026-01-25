package com.example.musicbooru.auth;

public record AuthenticationRequest(
        String username,
        String password
) {
}