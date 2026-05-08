package com.example.musicbooru.util;

import java.util.Map;

public final class Constants {

    public static final int PUBLIC_ID_LENGTH = 7;
    public static final String ARTWORK_EXTENSION = ".jpg";

    public static final Map<String, MediaType> SUPPORTED_MEDIA_TYPES = Map.of(
            "FLAC", new MediaType("audio/flac", ".flac"),
            "MP3", new MediaType("audio/mpeg", ".mp3"),
            "AAC", new MediaType("audio/mp4", ".m4a"),
            "OPUS", new MediaType("audio/ogg", ".ogg"),
            "VORBIS", new MediaType("audio/ogg", ".ogg")
    );

    public static final String FALLBACK_MEDIA_TYPE = "FLAC";

    private Constants() {
    }
}
