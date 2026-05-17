package com.example.musicbooru.auth;

import com.example.musicbooru.exception.InvalidCredentialsException;
import com.example.musicbooru.jwt.JwtService;
import com.example.musicbooru.user.CustomUserDetailsService;
import com.example.musicbooru.user.User;
import com.example.musicbooru.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return new AuthResponse(
                    null,
                    HttpStatus.CONFLICT,
                    "Username '" + request.username() + "' is taken"
            );
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        log.info("Registered user '{}' as '{}'",  request.username(), request.role());

        return new AuthResponse(
                bakeCookie(token),
                HttpStatus.OK,
                "Registered successfully"
        );
    }

    public AuthResponse logIn(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
            String token = jwtService.generateToken(userDetails);

            return new AuthResponse(
                    bakeCookie(token),
                    HttpStatus.OK,
                    "Logged in"
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Incorrect username or password");
        }
    }

    public AuthResponse logOut() {
        return new AuthResponse(
                crumbleCookie(),
                HttpStatus.NO_CONTENT,
                null
        );
    }

    private String bakeCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(jwtExpiration))
                .build()
                .toString();
    }

    private String crumbleCookie() {
        return ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }
}
