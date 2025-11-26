package com.example.musicbooru.exception;

import java.util.NoSuchElementException;

public class TrackNotFoundException extends NoSuchElementException {
    public TrackNotFoundException(String message) {
        super(message);
    }

    public TrackNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}