package com.example.musicbooru.track;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/track")
public class TrackController {

    private final TrackService trackService;

    @PostMapping
    public ResponseEntity<List<Track>> uploadTracks(@RequestParam("file") List<MultipartFile> files) {
        List<Track> tracks = trackService.addTracks(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(tracks);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteTracks(@RequestBody DeleteTracksRequest request) {
        trackService.removeTracks(request.publicTrackIds());
        return ResponseEntity.noContent().build();
    }

    @Profile("dev")
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllTracks() {
        trackService.removeAllTracks();
        return ResponseEntity.noContent().build();
    }
}
