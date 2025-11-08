package com.example.musicbooru.controller;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.io.IOException;
import java.io.FileNotFoundException;

@AllArgsConstructor
@RestController
@RequestMapping("/api/")
public class StreamController {

    private final TrackService trackService;

    @GetMapping("/stream/{id}")
    public ResponseEntity<StreamingResponseBody> streamTrack(
            @PathVariable String id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            Optional<Track> track = trackService.getTrackById(id);

            StreamingResponseBody responseStream;
            // TODO Implement extension derivation from content type
            Path filePath = Paths.get("./tracks/" + track.orElseThrow().getFileName());
            long fileSize = Files.size(filePath);
            byte[] buffer = new byte[8192];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if(rangeHeader == null) {
                // TODO Implement content type derivation
                responseHeaders.add("Content-Type", "audio/flac");
                responseHeaders.add("Content-Length", Long.toString(fileSize));
                responseStream = os -> {
                    try(RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
                        long pos = 0;
                        file.seek(pos);
                        while(pos < fileSize - 1) {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch(RuntimeException ignored) {}
                };

                return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            long rangeStart = Long.parseLong(ranges[0].substring(6));
            long rangeEnd;

            if(ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = fileSize - 1;
            }

            if(fileSize < rangeEnd) {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            // TODO Implement content type derivation
            responseHeaders.add("Content-Type", "audio/flac");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);
            final long _rangeEnd = rangeEnd;
            responseStream = os -> {
                try(RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
                    long pos = rangeStart;
                    file.seek(pos);
                    while(pos < _rangeEnd) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                } catch(RuntimeException ignored) {}
            };

            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        } catch(FileNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch(IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
