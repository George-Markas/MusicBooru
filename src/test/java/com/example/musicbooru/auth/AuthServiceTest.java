package com.example.musicbooru.auth;

import com.example.musicbooru.exception.InvalidCredentialsException;
import com.example.musicbooru.jwt.JwtService;
import com.example.musicbooru.user.CustomUserDetailsService;
import com.example.musicbooru.user.Role;
import com.example.musicbooru.user.User;
import com.example.musicbooru.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    CustomUserDetailsService userDetailsService;

    @Mock
    JwtService jwtService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    private final String username = "TestUser";
    private final String password = "plain-password";
    private final Role role = Role.USER;

    private final String encodedPassword = "encoded-password";
    private final String token = "jwt-token";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(username, password, role);
        authRequest = new AuthRequest(username, password);
    }

    // --- register ---

    @Test
    void registerUser_savesUserAndReturnsSuccessResponse_whenUsernameNotTaken() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        AuthResponse result = authService.register(registerRequest);

        assertThat(result.cookie().contains("jwt=" + token));
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.message()).isEqualTo("Registered successfully");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_encodesPasswordBeforeSavingUser() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        authService.register(registerRequest);

        verify(userRepository).save(argThat(user -> encodedPassword.equals(user.getPassword())));
    }

    @Test
    void register_returnsFailResponse_whenUsernameTaken() {
        when(userRepository.existsByUsername(username)).thenReturn(true);

        AuthResponse result = authService.register(registerRequest);

        assertThat(result.cookie()).isNull();
        assertThat(result.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.message()).isEqualTo("Username '" + registerRequest.username() + "' is taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_setsCorrectRoleForUser() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        authService.register(registerRequest);

        verify(userRepository).save(argThat(user -> user.getRole().equals(Role.USER)));

    }

    // --- logIn ---

    @Test
    void logIn_returnsSuccessResponse_whenCredentialsValid() {
        User user = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username(username)
                .password(password)
                .role(role)
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(token);

        AuthResponse result = authService.logIn(authRequest);

        assertThat(result.cookie().contains("jwt=" + token));
        assertThat(result.status()).isEqualTo(HttpStatus.OK);
        assertThat(result.message()).isEqualTo("Logged in");
    }

    @Test
    void logIn_throwsInvalidCredentialsException_whenCredentialsInvalid() {
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.logIn(authRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Incorrect username or password");

        verifyNoInteractions(userDetailsService);
    }

    @Test
    void logIn_passesCorrectCredentialsToAuthenticationManager() {
        User user = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username(username)
                .password(password)
                .role(role)
                .build();

        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(token);

        authService.logIn(authRequest);

        verify(authenticationManager).authenticate(
                argThat(tok -> tok instanceof UsernamePasswordAuthenticationToken t &&
                        username.equals(t.getPrincipal()) &&
                        password.equals(t.getCredentials())
                )
        );
    }

    // --- logOut ---

    @Test
    void logOut_returnsEmptyResponseAndCrumblesCookie() {
        AuthResponse result = authService.logOut();

        assertThat(result.cookie().contains("jwt=;"));
        assertThat(result.status()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.message()).isNullOrEmpty();
    }
}
