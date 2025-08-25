package com.example.musicbooru.controller;

import com.example.musicbooru.service.TrackService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/track-control")
public class TrackController {
    private final TrackService trackService;

    @PostMapping("/upload")
    public String uploadTrack(@RequestPart("file") MultipartFile audioFile) throws IOException {
        trackService.saveTrack(audioFile);
        return "success";
    }
}
