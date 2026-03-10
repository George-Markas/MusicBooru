package com.example.musicbooru.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.register(request);
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.authenticate(request);
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        AuthenticationResponse authenticationResponse = authenticationService.logout();
        return ResponseEntity.status(authenticationResponse.status())
                .header(HttpHeaders.SET_COOKIE, authenticationResponse.cookieString())
                .body(authenticationResponse);
    }
}
