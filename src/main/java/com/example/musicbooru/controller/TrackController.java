package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.TrackNotFoundException;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.example.musicbooru.util.Commons.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final Logger logger = LoggerFactory.getLogger(TrackController.class.getName());
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
        logger.error("Could not find track with ID {}", id);
        throw new TrackNotFoundException("Could not find track with ID " + id);
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

    // TODO Rewrite this properly using StreamController
//    @GetMapping("/stream/{id}")
//    public ResponseEntity<ResourceRegion> streamAudio(@PathVariable String id) throws IOException {
//        Optional<Track> track = Optional.ofNullable(trackService.getTrackById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
//
//        String path = LIBRARY + track.orElseThrow().getFileName();
//
//        UrlResource resource = new UrlResource(Paths.get(path).toUri());
//        long contentLength = resource.contentLength();
//
//        // Return the whole file
//        return ResponseEntity.ok()
//                .header("Accept-Ranges", "bytes")
//                .header("Content-Type", "audio/" + path.substring(path.lastIndexOf('.') + 1))
//                .body(new ResourceRegion(resource, 0, contentLength));
//    }

    @GetMapping("/art/{id}")
    public ResponseEntity<Resource> getArtwork(@PathVariable String id) {
        Optional<Track> track = trackService.getTrackById(id);
        if(track.isEmpty()) {
            logger.error("Could not fetch artwork; track with ID {} was not found", id);
            throw new TrackNotFoundException("Could not fetch artwork; track with ID " + id + " was not found");
        }

        try {
            Resource resource;
            if(track.get().isHasArtwork()) {
                Path path = Path.of(ARTWORK + id + ".webp");
                resource = new UrlResource(path.toUri());
            } else {
                resource = new ClassPathResource(NO_COVER);
                logger.warn("Could not find artwork for track with ID {}; using placeholder", id);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/webp"))
                    .body(resource);
        } catch(MalformedURLException e) {
            logger.error("Could not fetch artwork", e);
            throw new GenericException("Could not fetch artwork");
        }
    }
}
