package com.example.musicbooru.auth;

import com.example.musicbooru.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
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
    }

    // --- POST /auth/register ---

    @Test
    @WithMockUser(username = "TestAdmin", authorities = {"ADMIN"})
    void register_returnsOk_whenUserRegistrationSucceeds() throws Exception {

        RegisterRequest request = new RegisterRequest("TestUser", "plain_password", Role.USER);

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
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registered successfully"));
    }

    @Test
    @WithMockUser(username = "TestAdmin", authorities = {"ADMIN"})
    void register_returnsConflict_whenUsernameIsTaken() throws Exception {

        RegisterRequest request = new RegisterRequest("TestUser", "plain_password", Role.USER);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(
                        new AuthResponse(
                                null,
                                HttpStatus.CONFLICT,
                                "Username '" + request.username() + "' is taken"
                        )
                );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Username '" + request.username() + "' is taken"));
    }

    @Test
    @WithMockUser(username = "NaughtyTestUser", authorities = {"USER"})
    void register_returnsForbidden_whenUserIsNotAdmin() throws Exception {

        RegisterRequest request = new RegisterRequest("TestUser", "plain_password", Role.USER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    // --- POST /auth/login ---

}
