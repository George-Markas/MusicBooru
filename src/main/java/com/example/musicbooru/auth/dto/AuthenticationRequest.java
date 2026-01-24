package com.example.musicbooru.auth.dto;

public record AuthenticationRequest(
        String username,
        String password
) {
}