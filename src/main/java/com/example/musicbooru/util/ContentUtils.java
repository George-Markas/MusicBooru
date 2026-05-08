package com.example.musicbooru.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public class ContentUtils {

    private static final Tika tika = new Tika();

    private static final Map<String, String> PREFERRED_EXTENSION = Map.of(
            ".mp4a", ".m4a",
            ".mpga", ".mp3",
            ".ogx", ".ogg"
    );

    private static final Map<String, String> PREFERRED_MIMETYPE = Map.of(
            "audio/x-flac", "audio/flac",
            "application/ogg", "audio/ogg",
            "audio/vorbis", "audio/ogg"
    );

    /**
     * Detects the proper MIME type and extension for the given MultipartFile.
     *
     * @param file The MultipartFile to parse.
     * @return the MIME type and extension wrapped in a MediaType record.
     * @throws RuntimeException if the MultipartFile could not be read, or if the media type name is invalid.
     */
    public static MediaType detectMediaType(MultipartFile file) {
        try {
            String mimeType = tika.detect(file.getBytes());
            String extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();

            return new MediaType(
                    PREFERRED_MIMETYPE.getOrDefault(mimeType, mimeType),
                    PREFERRED_EXTENSION.getOrDefault(extension, extension)
            );
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Could not detect media type", e);
        }
    }
}
