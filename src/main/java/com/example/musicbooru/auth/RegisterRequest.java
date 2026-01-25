package com.example.musicbooru.auth;

public record RegisterRequest(
        String username,
        String password
) {
}