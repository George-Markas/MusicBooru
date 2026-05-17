package com.example.musicbooru.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Username can not be empty")
        String username,

        @NotBlank(message = "Password can not be empty")
        String password
) {
}
