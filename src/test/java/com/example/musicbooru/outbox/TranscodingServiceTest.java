package com.example.musicbooru.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TranscodingServiceTest {

    private TranscodingService transcodingService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        transcodingService = new TranscodingService();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initFakeFFmpeg(int exitCode) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path fakeFFmpeg;

        if (isWindows) {
            fakeFFmpeg = tempDir.resolve("ffmpeg.cmd");
            Files.writeString(fakeFFmpeg, "@echo off\nexit /b " + exitCode + "\r\n");
        } else {
            fakeFFmpeg = tempDir.resolve("ffmpeg");
            Files.writeString(fakeFFmpeg, "#!/bin/sh\nexit " + exitCode + "\n");
            fakeFFmpeg.toFile().setExecutable(true);
        }

        ReflectionTestUtils.setField(transcodingService, "ffmpegPath", fakeFFmpeg.toString());
    }

    @Test
    void transcode_returnsOutputFile_onSuccess() throws IOException, InterruptedException {
        initFakeFFmpeg(0);
        Path inputFile = Files.createTempFile(tempDir, null, ".wav");

        Path result = transcodingService.transcode(inputFile);

        assertNotNull(result);
        assertTrue(Files.exists(result));
        Files.deleteIfExists(result);
    }

    @Test
    void transcode_throwsIOException_whenFFmpegFails() throws IOException {
        initFakeFFmpeg(1);
        Path inputFile = Files.createTempFile(tempDir, null, ".wav");

        assertThrows(IOException.class, () -> transcodingService.transcode(inputFile));
    }

    @Test
    void transcode_deletesOutputFile_whenFFmpegFails() throws IOException {
        initFakeFFmpeg(1);
        Path inputFile = Files.createTempFile(tempDir, null, ".wav");

        Set<Path> before;
        try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            before = stream.collect(Collectors.toSet());
        }

        assertThrows(IOException.class, () -> transcodingService.transcode(inputFile));

        try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            Set<Path> after = stream.collect(Collectors.toSet());
            after.removeAll(before);
            assertTrue(after.isEmpty(), "Leftover temp files: " + after);
        }
    }

    @Test
    void transcode_usesConfiguredFFmpegPath() throws IOException, InterruptedException {
        initFakeFFmpeg(0);
        Path inputFile = Files.createTempFile(tempDir, null, ".wav");

        Path result = transcodingService.transcode(inputFile);
        assertNotNull(result);
        Files.deleteIfExists(result);
    }
}
