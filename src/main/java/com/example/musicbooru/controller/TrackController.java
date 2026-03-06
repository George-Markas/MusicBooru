package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.example.musicbooru.util.Commons.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final TrackService trackService;

    @GetMapping("/")
    public ResponseEntity<List<Track>> getTracks() {
        return ResponseEntity.ok(trackService.getTracks());
    }

    @GetMapping("/sort/{by}")
    public ResponseEntity<List<Track>> getTracks(@PathVariable String by) {
        return switch (by) {
            case "album" -> ResponseEntity.ok(trackService.getTracks("album"));
            case "artist" -> ResponseEntity.ok(trackService.getTracks("artist"));
            default -> ResponseEntity.ok(trackService.getTracks("title"));
        };
    }

    @GetMapping("/{trackId}")
    public ResponseEntity<Track> getTrack(@PathVariable String trackId) {
        Track track = trackService.getTrackById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));

        return ResponseEntity.ok(track);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTrack(@RequestPart("file") MultipartFile file) {
        trackService.addTrack(file);

        return ResponseEntity.ok("Track uploaded");
    }

    @DeleteMapping("/delete/{trackId}")
    public ResponseEntity<String> deleteTrack(@PathVariable String trackId) {
        trackService.removeTrack(trackId);

        return ResponseEntity.ok("Track deleted");
    }

    @GetMapping("/art/{trackId}")
    public ResponseEntity<Resource> getArtwork(@PathVariable String trackId) {
        if (!trackService.trackExists(trackId)) {
            throw new ResourceNotFoundException("Could not fetch artwork; track '" + trackId + "' not found");
        }

        try {
            Path artwork = Path.of(ARTWORK + trackId + ARTWORK_EXTENSION);
            Resource resource = (Files.exists(artwork)) ? new UrlResource(artwork.toUri()) : new ClassPathResource(NO_COVER);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new GenericException("Could not fetch artwork");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Track>> searchTracks(@RequestParam String query) {
        List<Track> results = trackService.searchTracks(query);

        return ResponseEntity.ok(results);
    }


    @Profile("dev")
    @PostMapping("/upload/batch")
    public ResponseEntity<String> uploadTracks(@RequestParam("file") List<MultipartFile> files) {
        for (MultipartFile file : files) {
            trackService.addTrack(file);
        }

        return ResponseEntity.ok(files.size() + " tracks uploaded");
    }

    @Profile("dev")
    @DeleteMapping("/delete/purge")
    public ResponseEntity<String> deleteAllTracks() {
        List<Track> tracks = trackService.getTracks();
        for (Track track : tracks) {
            trackService.removeTrack(String.valueOf(track.getId()));
        }

        return ResponseEntity.ok("Purged all tracks");
    }
}
