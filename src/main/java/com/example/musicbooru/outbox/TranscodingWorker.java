package com.example.musicbooru.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.example.musicbooru.util.Constants.*;

@Component
@Slf4j
public class TranscodingWorker {

    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;

    public Path transcode(Path inputFile) throws IOException, InterruptedException {
        Path outputFile = Files.createTempFile(null, SUPPORTED_MEDIA_TYPES.get(FALLBACK_MEDIA_TYPE).extension());

        //noinspection ExtractMethodRecommender
        List<String> ffmpegCommandLineArgs = List.of(
                ffmpegPath,
                "-i", inputFile.toAbsolutePath().toString(),
                "-c:a", "flac",
                "-compression_level", "8",
                "-map_metadata", "-1",
                "-y",
                outputFile.toAbsolutePath().toString()
        );

        ProcessBuilder ffmpegProcess = new ProcessBuilder(ffmpegCommandLineArgs);
        ffmpegProcess.redirectErrorStream(true);

        Process process = ffmpegProcess.start();

        String ffmpegOutput;
        try (InputStream inputStream = process.getInputStream()) {
            ffmpegOutput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("FFmpeg failed with exit code {}. Output:\n{}", exitCode, ffmpegOutput);
            Files.deleteIfExists(outputFile);
            throw new IOException("FFmpeg process exited with exit code " + exitCode);
        }

        log.debug("FFmpeg completed successfully. Output:\n{}", ffmpegOutput);
        return outputFile;
    }
}
