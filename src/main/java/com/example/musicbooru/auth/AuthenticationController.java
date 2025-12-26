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
        AuthenticationResponse authRes = service.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authRes.getCookieString())
                .body("Registered and logged in.");
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authRes = service.authenticate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authRes.getCookieString())
                .body("Login Success.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        AuthenticationResponse authRes = service.logout();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authRes.getCookieString())
                .body("Cookie purged.");
    }

}
