package com.example.musicbooru.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(response.status()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> logIn(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.logIn(request);
        return ResponseEntity.status(response.status())
                .header(HttpHeaders.SET_COOKIE, response.cookie())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logOut() {
        AuthResponse response = authService.logOut();
        return ResponseEntity.status(response.status())
                .header(HttpHeaders.SET_COOKIE, response.cookie())
                .body(response);
    }
}
