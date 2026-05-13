package com.example.musicbooru.auth;

import com.example.musicbooru.config.SecurityConfig;
import com.example.musicbooru.exception.GlobalExceptionHandler;
import com.example.musicbooru.exception.InvalidCredentialsException;
import com.example.musicbooru.jwt.JwtAuthFilter;
import com.example.musicbooru.jwt.JwtService;
import com.example.musicbooru.user.CustomUserDetailsService;
import com.example.musicbooru.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    private final String token = "jwt_token";

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(
                            invocation.getArgument(0, ServletRequest.class),
                            invocation.getArgument(1, ServletResponse.class)
                    );

            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        registerRequest = new RegisterRequest("TestUser", "plain_password", Role.USER);
        authRequest = new AuthRequest("TestUser", "plain_password");
    }

    // --- POST /auth/register ---

    @Test
    @WithMockUser(username = "TestAdmin", authorities = {"ADMIN"})
    void register_returnsOk_whenUserRegistrationSucceeds() throws Exception {


        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(
                        new AuthResponse(
                                token,
                                HttpStatus.OK,
                                "Registered successfully"
                        )
                );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registered successfully"));
    }

    @Test
    @WithMockUser(username = "TestAdmin", authorities = {"ADMIN"})
    void register_returnsConflict_whenUsernameIsTaken() throws Exception {


        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(
                        new AuthResponse(
                                null,
                                HttpStatus.CONFLICT,
                                "Username '" + registerRequest.username() + "' is taken"
                        )
                );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Username '" + registerRequest.username() + "' is taken"));
    }

    @Test
    @WithMockUser(username = "NaughtyTestUser", authorities = {"USER"})
    void register_returnsForbidden_whenUserIsNotAdmin() throws Exception {


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isForbidden());
    }

    // --- POST /auth/login ---

    @Test
    void logIn_returnsOk_whenCredentialsAreValid() throws Exception {

        when(authService.logIn(any(AuthRequest.class)))
                .thenReturn(
                        new AuthResponse(
                                "jwt=" + token,
                                HttpStatus.OK,
                                "Login successful"
                        )
                );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(header().string(HttpHeaders.SET_COOKIE, "jwt=" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void logIn_returnsUnauthorized_whenCredentialsAreInvalid() throws Exception {
        when(authService.logIn(any(AuthRequest.class)))
                .thenThrow(new InvalidCredentialsException("Incorrect username or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect username or password"));
    }

    // --- POST /auth/logout ---

    @Test
    @WithMockUser(username = "TestUser", authorities = {"USER"})
    void logOut_returnsNoContent_whenUserIsAuthenticated() throws Exception {
        when(authService.logOut())
                .thenReturn(
                        new AuthResponse(
                                "jwt=",
                                HttpStatus.NO_CONTENT,
                                null
                        )
                );

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, "jwt="))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void logOut_returnsForbidden_whenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isForbidden());
    }

}
