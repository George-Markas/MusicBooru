package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

import static com.example.musicbooru.util.Commons.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final static Logger logger = LoggerFactory.getLogger(TrackController.class);
    private final TrackService trackService;

    @GetMapping("/")
    public ResponseEntity<List<Track>> getAllTracks() {
        return ResponseEntity.ok(trackService.getTracks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable String id) {
        Optional<Track> track = trackService.getTrackById(id);

        if (track.isPresent()) {
            return ResponseEntity.ok(track.get());
        }

        logger.error("Could not find track '{}'", id);
        throw new ResourceNotFoundException("Could not find track '" + id + "'");
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTrack(@RequestPart("file") MultipartFile file) {
        trackService.uploadTrack(file);
        return ResponseEntity.ok("Track uploaded");
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteTrack(@PathVariable String id) {
        trackService.deleteTrack(id);
        return ResponseEntity.ok("Track deleted");
    }

    @GetMapping("/art/{id}")
    public ResponseEntity<Resource> getArtwork(@PathVariable String id) {
        if (!trackService.trackExists(id)) {
            logger.error("Could not fetch artwork; track '{}' not found", id);
            throw new ResourceNotFoundException("Could not fetch artwork; track '" + id + "' not found");
        }

        try {
            Resource resource;
            Path path = Path.of(ARTWORK + id + ARTWORK_EXTENSION);
            if (Files.exists(path)) {
                resource = new UrlResource(path.toUri());
            } else {
                resource = new ClassPathResource(NO_COVER);
                logger.warn("Could not find artwork for track '{}'; using placeholder", id);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (MalformedURLException e) {
            logger.error("Could not fetch artwork", e);
            throw new GenericException("Could not fetch artwork");
        }
    }
}
