package com.example.musicbooru.util;

public class Commons {
    private Commons() {} // No need to instantiate, hide the constructor

    public final static String LIBRARY = "./library/";
    public final static String ARTWORK = "./artwork/";
    public final static String NO_COVER = "static/no_cover.jpg";

    // TODO Support more audio formats
    public final static String FILE_EXTENSION = ".flac";
    public final static String MEDIA_TYPE_FLAC = "audio/flac"; // Not "audio/x-flac"
}