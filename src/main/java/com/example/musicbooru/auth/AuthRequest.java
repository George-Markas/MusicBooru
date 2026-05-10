package com.example.musicbooru.auth;

public record AuthRequest(
        String username,
        String password
) {
}
