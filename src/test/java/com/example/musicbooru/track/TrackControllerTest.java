package com.example.musicbooru.track;

import com.example.musicbooru.auth.AuthRequest;
import com.example.musicbooru.auth.RegisterRequest;
import com.example.musicbooru.config.SecurityConfig;
import com.example.musicbooru.exception.GlobalExceptionHandler;
import com.example.musicbooru.jwt.JwtAuthFilter;
import com.example.musicbooru.jwt.JwtService;
import com.example.musicbooru.user.CustomUserDetailsService;
import com.example.musicbooru.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
@Import(SecurityConfig.class)
public class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private TrackService trackService;

    private Track track;

    @BeforeEach
    public void setUp() {
        track = Track.builder()
                .id(1L)
                .publicId("IG1MNki")
                .artist("Artist")
                .title("Title")
                .album("Album")
                .year("2010-11-22")
                .genre("Genre")
                .duration(292)
                .mimeType("audio/flac")
                .status(TrackStatus.READY)
                .build();
    }

    // --- POST uploadTracks ----

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void uploadTracks_returnsCreatedWithTrackList() throws Exception {
        when(trackService.addTracks(any())).thenReturn(List.of(track));

        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "test-audio-file",
                "test.flac",
                "audio/flac",
                "audio-data".getBytes()
        );

        mockMvc.perform(multipart("/track").file(mockMultipartFile).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].title").value(track.getTitle()));
    }
}
