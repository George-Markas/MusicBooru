package com.example.musicbooru.controller;

import com.example.musicbooru.dto.AddTrackToPlaylistRequest;
import com.example.musicbooru.dto.CreatePlaylistRequest;
import com.example.musicbooru.dto.PlaylistResponse;
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

    @PostMapping("/{playlistId}/track")
    public ResponseEntity<PlaylistResponse> addTrack(
            @PathVariable String playlistId,
            @RequestBody AddTrackToPlaylistRequest request,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(playlistService.addTrack(playlistId, request.trackId(), user));
    }

    @DeleteMapping("/{playlistId}/track/{entryId}")
    public ResponseEntity<PlaylistResponse> removeTrack(
            @PathVariable String playlistId,
            @PathVariable String entryId,
            @AuthenticationPrincipal User user) {

        playlistService.removeTrack(playlistId, entryId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Playlist> deletePlaylist(
            @PathVariable String playlistId,
            @AuthenticationPrincipal User user) {

        playlistService.deletePlaylist(playlistId, user);
        return ResponseEntity.noContent().build();
    }
}