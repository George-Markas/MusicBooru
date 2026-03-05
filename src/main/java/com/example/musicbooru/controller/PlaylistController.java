package com.example.musicbooru.controller;

import com.example.musicbooru.dto.CreatePlaylistRequest;
import com.example.musicbooru.model.Playlist;
import com.example.musicbooru.model.User;
import com.example.musicbooru.service.PlaylistService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {
    private final PlaylistService playlistService;

    @PostMapping
    ResponseEntity<Playlist> createPlaylist(
            @RequestBody CreatePlaylistRequest request,
            @AuthenticationPrincipal User user) {

        Playlist playlist = playlistService.createPlaylist(user, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    @GetMapping
    public ResponseEntity<List<Playlist>> getPlaylists(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(playlistService.getPlaylistsByOwner(user));
    }

    @PostMapping("/{playlistId}/add/{trackId}")
    public ResponseEntity<Playlist> addTrackToPlaylist(
            @PathVariable String playlistId,
            @PathVariable String trackId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(playlistService.addTrackToPlaylist(playlistId, trackId, user));
    }

    @DeleteMapping("/{playlistId}/remove/{trackId}")
    public ResponseEntity<Playlist> removeTrackFromPlaylist(
            @PathVariable String playlistId,
            @PathVariable String trackId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(playlistService.removeTrackFromPlaylist(playlistId, trackId, user));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Playlist> deletePlaylist(
            @PathVariable String playlistId,
            @AuthenticationPrincipal User user) {

        playlistService.deletePlaylist(playlistId, user);
        return ResponseEntity.noContent().build();
    }
}