package com.example.musicbooru.util;

import com.example.musicbooru.exception.GenericException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class HeaderUtils {

    private static final Logger logger = LoggerFactory.getLogger(HeaderUtils.class.getName());

    public static String generateETag(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return String.format("\"%d-%d\"", attrs.lastModifiedTime().toMillis(), attrs.size());
        } catch(IOException e) {
            logger.error("Could not read file attributes", e);
            throw new GenericException("Could not read file attributes");
        }
    }

    public static boolean matches(String etag, String header) {
        if(header == null || header.isBlank()) {
            return false;
        }

        for(String eTagValue : header.split(",")) {
            String current = eTagValue.trim();
            if("*".equals(current)) return true; // Matches any resource
            if(current.startsWith("W/")) current = current.substring(2).trim(); // Accept weak entity tags
            if(current.equals(etag)) return true; // Matches specified ETag
        }

        return false;
    }

    public static Instant parseHttpDate(String httpDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return ZonedDateTime.parse(httpDate, formatter).toInstant();
        } catch(DateTimeParseException e) {
            logger.error("Could not parse date string", e);
            throw new GenericException("Could not parse date string");
        }
    }
}
