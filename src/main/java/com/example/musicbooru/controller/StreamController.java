package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import com.example.musicbooru.util.HeaderUtils;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.example.musicbooru.util.Commons.AUDIO_MIMETYPE;
import static com.example.musicbooru.util.Commons.LIBRARY;

@RestController
@AllArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final TrackService trackService;

    @GetMapping("/{trackId}")
    public ResponseEntity<Resource> serveResource(
            @PathVariable String trackId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
            @RequestHeader(value = HttpHeaders.IF_UNMODIFIED_SINCE, required = false) String ifUnmodifiedSince) {

        Track track = trackService.getTrackById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));

        String fileName = track.getFileName();

        Path filePath = Path.of(LIBRARY + fileName);
        if (Files.notExists(filePath)) {
            throw new ResourceNotFoundException("Audio file '" + filePath + "' not found");
        }

        try {
            // We generate the ETag from file metadata as opposed to something like hashing the file
            // because we're dealing with relatively large files.
            String eTag = HeaderUtils.generateETag(filePath);
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();


            // --- Validate request headers for caching ---

            // If-None-Match header should contain "*" or ETag. If so, return 304
            if (ifNoneMatch != null && HeaderUtils.matches(ifNoneMatch, eTag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(eTag)
                        .lastModified(lastModified)
                        .build();
            }

            // If-Modified-Since header should be greater than lastModified. If so, return 304
            // This header is ignored if any If-None-Match header is specified.
            if (ifNoneMatch == null && ifModifiedSince != null) {
                Instant clientTimestamp = HeaderUtils.parseHttpDate(ifModifiedSince);
                if (clientTimestamp.isAfter(lastModified)) {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .eTag(eTag)
                            .lastModified(lastModified)
                            .build();
                }
            }

            // --- Validate request headers for resume ---

            // If-Match header should contain "*" or ETag. If not, return 412
            if (ifMatch != null && !HeaderUtils.matches(ifMatch, eTag)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
            }

            // If-Unmodified-Since header should be greater than lastModified. If not, return 412
            if (ifMatch == null && ifUnmodifiedSince != null) {
                Instant clientTimestamp = HeaderUtils.parseHttpDate(ifUnmodifiedSince);
                if (clientTimestamp.isBefore(lastModified)) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
                }
            }

            // --- Content phase ---

            // Prepare the resource response
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(AUDIO_MIMETYPE))
                    .contentLength(filePath.toFile().length())
                    .eTag(eTag)
                    .lastModified(lastModified)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new GenericException("An unexpected error occurred");
        }
    }
}