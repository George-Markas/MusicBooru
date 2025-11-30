package com.example.musicbooru.util;

import com.example.musicbooru.exception.GenericException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class ETagGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ETagGenerator.class.getName());

    // We're opting to generate the ETag from metadata as opposed to hashing the audio file
    // since we're dealing with relatively large files (30MB - 100MB)

    public static String generateETag(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return String.format("\"%d-%d\"", attrs.lastModifiedTime().toMillis(), attrs.size());
        } catch(IOException e) {
            logger.error("Could not read file attributes", e);
            throw new GenericException("Could not read file attributes", e);
        }
    }
}
