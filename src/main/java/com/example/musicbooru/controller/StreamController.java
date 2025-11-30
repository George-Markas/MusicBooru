package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import com.example.musicbooru.util.HeaderUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.example.musicbooru.util.Commons.LIBRARY;
import static com.example.musicbooru.util.Commons.MEDIA_TYPE_FLAC;

@AllArgsConstructor
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

    private final TrackService trackService;

    @GetMapping("/{id}")
    public ResponseEntity<Resource> serveResource(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
            @RequestHeader(value = HttpHeaders.IF_UNMODIFIED_SINCE, required = false) String ifUnmodifiedSince
    ) {
        Optional<Track> track = trackService.getTrackById(id);
        if(track.isEmpty()) {
            logger.error("Could not find track with ID {}", id);
            throw new ResourceNotFoundException("Could not find track with ID " + id);
        }

        String fileName = track.orElseThrow(() -> {
            logger.error("Could not get filename for track with ID {}", id);
            return new ResourceNotFoundException("Could not get filename for track with ID " + id);
        }).getFileName();

        Path filePath = Path.of(LIBRARY + fileName);
        if(Files.notExists(filePath)) {
            logger.error("Could not find audio file with path {} for track with ID {}", filePath, id);
            throw new ResourceNotFoundException("Could not find audio file with path" + filePath + "for track with ID " + id);
        }

        try {
            // We generate the ETag from file metadata as opposed to something like hashing the file
            // because we're dealing with relatively large files (30 - 100 MB)
            String eTag = HeaderUtils.generateETag(filePath);
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();


            // --- Validate request headers for caching ---

            // If-None-Match header should contain "*" or ETag. If so, return 304
            if(ifNoneMatch != null && HeaderUtils.matches(ifNoneMatch, eTag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(eTag)
                        .lastModified(lastModified)
                        .build();
            }

            // If-Modified-Since header should be greater than lastModified. If so, return 304
            // This header is ignored if any If-None-Match header is specified
            if(ifNoneMatch == null && ifModifiedSince != null) {
                Instant clientTimestamp = HeaderUtils.parseHttpDate(ifModifiedSince);
                if(clientTimestamp.isAfter(lastModified)) {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .eTag(eTag)
                            .lastModified(lastModified)
                            .build();
                }
            }

            // --- Validate request headers for resume ---

            // If-Match header should contain "*" or ETag. If not, return 412
            if(ifMatch != null && !HeaderUtils.matches(ifMatch, eTag)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
            }

            // If-Unmodified-Since header should be greater than lastModified. If not, return 412
            if(ifMatch == null && ifUnmodifiedSince != null) {
                Instant clientTimestamp = HeaderUtils.parseHttpDate(ifUnmodifiedSince);
                if(clientTimestamp.isBefore(lastModified)) {
                   return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
                }
            }

            // --- Content phase ---

            // Prepare the resource response
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(MEDIA_TYPE_FLAC))
                    .contentLength(filePath.toFile().length())
                    . eTag(eTag)
                    .lastModified(lastModified)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch(IOException e) {
            logger.error("An unexpected error occurred", e);
            throw new GenericException("An unexpected error occurred");
        }
    }
}