package com.example.musicbooru.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GenericException extends RuntimeException {
    private final HttpStatus status;

    public GenericException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public GenericException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}