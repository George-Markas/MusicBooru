package com.example.musicbooru.auth;

import com.example.musicbooru.config.JwtService;

import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;

import com.example.musicbooru.model.UserAuthView;
import com.example.musicbooru.repository.UserRepository;
import com.example.musicbooru.repository.UserAuthViewRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    
    private static final int COOKIE_LIFESPAN = 900;
    
    private final UserRepository repository;
    private final UserAuthViewRepository authViewRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            UserAuthViewRepository authViewRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.repository = userRepository;
        this.authViewRepository = authViewRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse register(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        repository.save(user);
        String jwtToken = jwtService.generateToken(user);
        String jwtCookieString = jwtService.cookieFromToken(jwtToken, COOKIE_LIFESPAN);
        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserAuthView userAuth = authViewRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(request.getUsername()));

        String jwtToken = jwtService.generateToken(userAuth);
        String jwtCookieString = jwtService.cookieFromToken(jwtToken, COOKIE_LIFESPAN);

        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .build();
    }

    public AuthenticationResponse logout() {
        String jwtCookieString = jwtService.logoutCookie();
        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .build();
    }

}