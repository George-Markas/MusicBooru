package com.example.musicbooru.auth;

import com.example.musicbooru.jwt.JwtService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    private final String username = "KinkyMango92";
    private final String password = "supersecretpassword";
    private final Role role = Role.USER;

    private final String encodedPassword = "$2a$10$c1oP/MpcptgceYVcxkFMxOZYzuvuix9WhZMiffX70nQYXdgCow5La";
    private final String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJLaW5reU1hbmdvOTIiLCJhdXRob3JpdG" +
                                 "llcyI6W3siYXV0aG9yaXR5IjoiVVNFUiJ9XSwiaWF0IjoxNzc4NTk5NDc2LCJle" +
                                 "HAiOjE3Nzg2ODU4NzZ9._iz4l1k8wGrPTLfAGWxbO8mZKg8IJ0OPH7SBDLBae5s";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(username, password, role);
    }

    // --- register ---

    @Test
    void registerUser_savesUserAndReturnsOk_whenUsernameIsNotTaken() {
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
    void register_returnsConflict_whenUsernameIsTaken() {
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
    void logIn_returnsOk_whenCredentialsAreValid() {
    }
}
