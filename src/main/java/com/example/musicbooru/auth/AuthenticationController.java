package com.example.musicbooru.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse authenticationResponse = service.register(request);
        return ResponseEntity.status(authenticationResponse.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.getCookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authenticationResponse = service.authenticate(request);
        return ResponseEntity.status(authenticationResponse.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.getCookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        AuthenticationResponse authenticationResponse = service.logout();
        return ResponseEntity.status(authenticationResponse.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.getCookieString())
                .body(authenticationResponse);
    }
}
