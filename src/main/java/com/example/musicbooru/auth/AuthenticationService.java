package com.example.musicbooru.auth;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;
import com.example.musicbooru.model.UserAuthView;
import com.example.musicbooru.repository.UserRepository;
import com.example.musicbooru.repository.UserAuthViewRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final UserAuthViewRepository authViewRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        if (repository.existsByUsername(user.getUsername())) {
            throw new GenericException("Username already in use", HttpStatus.CONFLICT);
        }

        repository.save(user);
        String jwtToken = jwtService.generateToken(user);
        String jwtCookieString = jwtService.cookieFromToken(jwtToken);

        return new AuthenticationResponse(
                jwtCookieString,
                HttpStatus.OK,
                "User registered successfully");
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new GenericException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

        // TODO Evaluate whether this check is needed or not
        Optional<UserAuthView> userAuth = authViewRepository.findByUsername(request.username());
        if (userAuth.isEmpty()) {
            throw new GenericException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

        String jwtToken = jwtService.generateToken(userAuth.get());
        String jwtCookieString = jwtService.cookieFromToken(jwtToken);

        return new AuthenticationResponse(
                jwtCookieString,
                HttpStatus.OK,
                "Login success");
    }

    public AuthenticationResponse logout() {
        String jwtCookieString = jwtService.logoutCookie();
        return new AuthenticationResponse(
                jwtCookieString,
                HttpStatus.OK,
                "Logout success");
    }
}