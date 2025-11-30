package com.example.musicbooru.controller;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.TrackNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import com.example.musicbooru.util.ETagGenerator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.musicbooru.util.Commons.LIBRARY;
import static com.example.musicbooru.util.Commons.MEDIA_TYPE_FLAC;

@AllArgsConstructor
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

    private final TrackService trackService;

    @GetMapping("/{id}")
    public ResponseEntity<StreamingResponseBody> serveResource(
            @PathVariable String id,
            WebRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(value = HttpHeaders.IF_UNMODIFIED_SINCE, required = false) String ifUnmodifiedSince,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String Range,
            @RequestHeader(value = HttpHeaders.IF_RANGE, required = false) String ifRange
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
            String etag = ETagGenerator.generateETag(filePath);

            // --- Validate request headers for caching ---

            // If-None-Match header should contain "*" or ETag. If so, return 304
            if(request.checkNotModified(etag)) {
                return null; // Returns 304 automatically
            }

            // If-Modified-Since header should be greater than lastModified. If so, return 304
            // This header is ignored if any If-None-Match header is specified
            if(request.checkNotModified(lastModified.toEpochMilli())) {
                return null; // Returns 304 automatically
            }

            // --- Validate request headers for resume ---

            // If-Match header should contain "*" or ETag. If not, return 412
            if(HeaderUtils.matches(etag, ifMatch)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
            }

            // If-Unmodified-Since header should be greater than lastModified. If not, return 412
            if(ifUnmodifiedSince != null) {
                Instant clientTimestamp = HeaderUtils.parseHttpDate(ifUnmodifiedSince);
                if(lastModified.isAfter(clientTimestamp)) {
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
                }
            }

            // --- Validate and process range ---

            if(Range != null) {
                try {
                    List<HttpRange> ranges = new ArrayList<>();
                    ranges = HttpRange.parseRanges(Range);

                    // If-Range header is either malformed or contains a date (not an ETag)
                    // Either way, return 200 (full resource)
                    if(ifRange != null && !ifRange.equals(etag)) {

                    }

                } catch (IllegalArgumentException e) {
                    logger.error("Could not parse range header", e);
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .build();
                }
            }


        } catch(IOException e) {
            logger.error("An unexpected error occurred", e);
            throw new GenericException("An unexpected error occurred", e);
        }
    }

    private static class HeaderUtils {
        public static boolean matches(String etag, String header) {
            if(header == null || header.isBlank()) {
                return false;
            }

            for(String etagValue : header.split(",")) {
                String current = etagValue.trim();
                if(current.equals("*")) return true; // Matches any resource
                if(current.startsWith("W/")) current = current.substring(2).trim(); // Accept weak entity tags
                if(current.equals(etag)) return true; // Matches specified ETag
            }

            return false;
        }

        public static Instant parseHttpDate(String httpDate) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                return ZonedDateTime.parse(httpDate, formatter).toInstant();
            } catch (DateTimeParseException e) {
                logger.error("Could not parse date string", e);
                throw new GenericException("Could not parse date string", e);
            }
        }
    }
}