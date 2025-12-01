package com.example.musicbooru.util;

import com.example.musicbooru.exception.GenericException;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("LoggingSimilarMessage")
public class MediaUtils {

    private static final Logger logger = LoggerFactory.getLogger(MediaUtils.class.getName());

    private static final Map<String, String> extensions = new HashMap<>();

    static {
        extensions.put("audio/mpeg", ".mp3");
        extensions.put("audio/flac", ".flac");
        extensions.put("audio/mp4", ".m4a");
    }

    public static String detectMediaType(MultipartFile file) {
        try(InputStream inputStream = file.getInputStream()) {
            Tika tika = new Tika(TikaConfig.getDefaultConfig());
            String mediaType = tika.detect(inputStream);

            // Normalize audio/x-flac to standard audio/flac
            if("audio/x-flac".equals(mediaType)) {
                mediaType = "audio/flac";
            }

            logger.info("Detected media type of {}", mediaType);

            return mediaType;
        } catch(IOException e) {
            logger.error("Could not read file for detection", e);
            throw new GenericException("Could not read file for detection");
        }
    }

    public static String getExtension(String mediaType) {
        String extension = extensions.get(mediaType);
        if(extension != null) {
            return extension;
        }

        logger.error("Media type {} is not supported", mediaType);
        throw new GenericException("Media type " + mediaType + " is not supported");
    }
}
