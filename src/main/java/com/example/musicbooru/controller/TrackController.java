package com.example.musicbooru.controller;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/api/track")
public class TrackController {
    private final TrackService trackService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTrack(@RequestPart("file") MultipartFile audioFile) throws IOException {
        trackService.saveTrack(audioFile);
        return ResponseEntity.ok("Track uploaded.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable Integer id) {
        Optional<Track> track = trackService.getTrackById(id);

        if (track.isPresent()) {
            return ResponseEntity.ok(track.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/")
    public ResponseEntity<List<Track>> getAllTracks() {
        return ResponseEntity.ok(trackService.getTracks());
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<ResourceRegion> streamAudio(
            @PathVariable Integer id) throws IOException {

        Optional<Track> track = Optional.ofNullable(trackService.getTrackById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

        String path = track.orElseThrow().getPath();

        UrlResource resource = new UrlResource(Paths.get(path).toUri());
        long contentLength = resource.contentLength();

        // Return the whole file
        return ResponseEntity.ok()
                .header("Accept-Ranges", "bytes")
                .header("Content-Type", "audio/" + path.substring(path.lastIndexOf('.') + 1))
                .body(new ResourceRegion(resource, 0, contentLength));
    }
}
