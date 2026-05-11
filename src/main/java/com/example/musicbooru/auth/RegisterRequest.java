package com.example.musicbooru.auth;

import com.example.musicbooru.user.Role;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Password can not be empty")
        String username,

        @NotBlank(message = "Password can not be empty")
        String password,

        Role role
) {
}
