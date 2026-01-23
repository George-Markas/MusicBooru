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
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        repository.save(user);
        String jwtToken = jwtService.generateToken(user);

        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserAuthView userAuthView = authViewRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException(request.username()));

        String jwtToken = jwtService.generateToken(userAuthView);

        return new AuthenticationResponse(jwtToken);
    }
}