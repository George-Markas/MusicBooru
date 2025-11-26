package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.TrackNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static com.example.musicbooru.util.Commons.LIBRARY;
import static com.example.musicbooru.util.Commons.MEDIA_TYPE_FLAC;

@AllArgsConstructor
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final Logger logger = LoggerFactory.getLogger(StreamController.class);

    private final TrackService trackService;

    @GetMapping("/{id}")
    public ResponseEntity<StreamingResponseBody> streamResource(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatchHeader
    ) {

        Optional<Track> track = trackService.getTrackById(id);
        if(track.isEmpty()) {
            logger.error("Could not find track with ID {}", id);
            throw new TrackNotFoundException("Could not find track with ID " + id);
        }

        // This should never happen, as an exception would be thrown by uploadTrack() in TrackService
        // when attempting to upload an audio file with insufficient metadata to construct a filename from
        String fileName = track.orElseThrow(() -> {
            logger.error("Could not get filename for track with ID {}", id);
            return new GenericException("Could not get filename for track with ID " + id);
        }).getFileName();

        Path filePath = Path.of(LIBRARY + fileName);
        if(Files.notExists(filePath)) {
            logger.error("Could not find audio file with path {} for track with ID {}", filePath, id);
            throw new GenericException("Could not find audio file with path" + filePath + "for track with ID" + id);
        }

        try {
            long fileSize = Files.size(filePath);
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
            String contentType = MEDIA_TYPE_FLAC; // TODO Do this programmatically to support more formats

            // --- Validate request headers for caching ---

            // If-None-Match header should contain "*" or ETag. If so, return 304
            if(ifNoneMatchHeader != null && )

        } catch(IOException e) {
            logger.error("An unexpected error occurred", e);
            throw new GenericException("An unexpected error occurred", e);
        }
    }
}