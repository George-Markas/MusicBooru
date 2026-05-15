package com.example.musicbooru.track;

import com.example.musicbooru.config.SecurityConfig;
import com.example.musicbooru.jwt.JwtAuthFilter;
import com.example.musicbooru.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
@Import(SecurityConfig.class)
public class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private String trackPublicId;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(
                            invocation.getArgument(0, ServletRequest.class),
                            invocation.getArgument(1, ServletResponse.class)
                    );

            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());

        trackPublicId = "IG1MNki";
    }

    // --- POST uploadTracks ---

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadTracks_returnsCreated() throws Exception {
        Track track = Track.builder().publicId(trackPublicId).build();
        when(trackService.addTracks(anyList())).thenReturn(List.of(track));

        mockMvc.perform(multipart("/track")
                        .file("file", "audio".getBytes())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].publicId").value(trackPublicId));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void uploadTracks_returnsForbidden_whenUserIsNotAdmin() throws Exception {
        mockMvc.perform(multipart("/track")
                        .file("file", "audio".getBytes())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackService);
    }

    // --- DELETE deleteTracks ---

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteTracks_returnsNoContent() throws Exception {
        DeleteTracksRequest request = new DeleteTracksRequest(List.of(trackPublicId));

        mockMvc.perform(delete("/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNoContent());

        verify(trackService).removeTracks(List.of(trackPublicId));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteTracks_returnsNotFound_whenTrackIsNotFound() throws Exception {
        DeleteTracksRequest request = new DeleteTracksRequest(List.of(trackPublicId));

        mockMvc.perform(delete("/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNoContent());

        verify(trackService).removeTracks(List.of(trackPublicId));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void deleteTracks_returnsForbidden_whenUserIsNotAdmin() throws Exception {
        DeleteTracksRequest request = new DeleteTracksRequest(List.of(trackPublicId));

        mockMvc.perform(delete("/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(trackService);
    }
}
