package com.example.musicbooru.controller;

import com.example.musicbooru.dto.AddTrackToPlaylistRequest;
import com.example.musicbooru.dto.CreatePlaylistRequest;
import com.example.musicbooru.dto.PlaylistResponse;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Playlist;
import com.example.musicbooru.model.User;
import com.example.musicbooru.service.PlaylistService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.example.musicbooru.util.Commons.*;

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
    public ResponseEntity<PlaylistResponse> addTrackToPlaylist(
            @PathVariable String playlistId,
            @RequestBody AddTrackToPlaylistRequest request,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(playlistService.addTrack(playlistId, request.trackId(), user));
    }

    @DeleteMapping("/{playlistId}/track/{entryId}")
    public ResponseEntity<PlaylistResponse> removeTrackFromPlaylist(
            @PathVariable String playlistId,
            @PathVariable String entryId,
            @AuthenticationPrincipal User user) {

        playlistService.removeTrack(playlistId, entryId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{playlistId}/icon")
    public ResponseEntity<Resource> getPlaylistIcon(
            @PathVariable String playlistId,
            @AuthenticationPrincipal User user) {

        if (!playlistService.playlistExists(playlistId, user)) {
            throw new ResourceNotFoundException("Could not fetch icon; playlist '" + playlistId + "' not found");
        }

        try {
            Path icon = Path.of(ICON + playlistId + ICON_EXTENSION);
            Resource resource = (Files.exists(icon)) ? new UrlResource(icon.toUri()) : new ClassPathResource(NO_ICON);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new GenericException("Could not fetch icon");
        }
    }

    @PostMapping("/{playlistId}/icon")
    public ResponseEntity<?> setPlaylistIcon(
            @PathVariable String playlistId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        playlistService.addIcon(file, playlistId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}/icon")
    public ResponseEntity<?> removePlaylistIcon(
            @PathVariable String playlistId,
            @AuthenticationPrincipal User user) {

        playlistService.removeIcon(playlistId, user);
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