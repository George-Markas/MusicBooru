package com.example.musicbooru.controller;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final TrackService trackService;

    @GetMapping("/")
    public ResponseEntity<List<Track>> getAllTracks() {
        return ResponseEntity.ok(trackService.getTracks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable String id) {
        Optional<Track> track = trackService.getTrackById(id);

        if(track.isPresent()) {
            return ResponseEntity.ok(track.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTrack(@RequestPart("file") MultipartFile file) throws IOException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException {
        trackService.uploadTrack(file);
        return ResponseEntity.ok("Track uploaded.");
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteTrack(@PathVariable String id) {
        try {
            trackService.deleteTrack(id);
            return ResponseEntity.ok("Track deleted.");
        } catch(IOException | NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/art/{id}")
    public ResponseEntity<Resource> getCoverArt(@PathVariable String id) {
        Resource coverArt = new FileSystemResource("./tracks/covers/" + id + ".jpg");
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(coverArt);
    }
}
