package com.example.musicbooru.util;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class FileTypeDetector {

    public static String detectMediaType(MultipartFile file) throws TikaException, IOException {
        TikaConfig tikaConfig = new TikaConfig();
        Detector detector = tikaConfig.getDetector();
        Metadata metadata = new Metadata();

        InputStream inputStream = TikaInputStream.get(file.getInputStream());
        MediaType mediaType = detector.detect(inputStream, metadata);
        return mediaType.toString();

    }

    public static String detectFileExtension(MultipartFile file) throws IOException, TikaException {
        TikaConfig tikaConfig = new TikaConfig();
        Detector detector = tikaConfig.getDetector();
        Metadata metadata = new Metadata();

        InputStream inputStream = TikaInputStream.get(file.getInputStream());
        MediaType mediaType = detector.detect(inputStream, metadata);
        return tikaConfig.getMimeRepository()
                .forName(mediaType.toString())
                .getExtension();
    }
}
