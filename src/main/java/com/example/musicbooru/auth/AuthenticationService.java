package com.example.musicbooru.auth;

import com.example.musicbooru.config.JwtService;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.model.Role;
import com.example.musicbooru.model.User;
import com.example.musicbooru.model.UserAuthView;
import com.example.musicbooru.repository.UserRepository;
import com.example.musicbooru.repository.UserAuthViewRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService {
    private final static Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

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

        if (repository.existsByUsername(user.getUsername())) {
            logger.error("User registration failed; conflicting usernames");
            throw new GenericException("Username already in use", HttpStatus.CONFLICT);
        }

        repository.save(user);
        String jwtToken = jwtService.generateToken(user);
        String jwtCookieString = jwtService.cookieFromToken(jwtToken, COOKIE_LIFESPAN);

        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .statusCode(HttpStatus.OK)
                .responseMessage("User registered successfully")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            logger.error("Authentication failed for user '{}'", request.getUsername());
            throw new GenericException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

        // TODO Evaluate whether this check is needed or not
        Optional<UserAuthView> userAuth = authViewRepository.findByUsername(request.getUsername());
        if (userAuth.isEmpty()) {
            logger.error("Authentication failed for user '{}'", request.getUsername());
            throw new GenericException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

        String jwtToken = jwtService.generateToken(userAuth.get());
        String jwtCookieString = jwtService.cookieFromToken(jwtToken, COOKIE_LIFESPAN);

        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .statusCode(HttpStatus.OK)
                .responseMessage("Login success")
                .build();
    }

    public AuthenticationResponse logout() {
        String jwtCookieString = jwtService.logoutCookie();
        return AuthenticationResponse.builder()
                .cookieString(jwtCookieString)
                .statusCode(HttpStatus.OK)
                .responseMessage("Logged out")
                .build();
    }
}