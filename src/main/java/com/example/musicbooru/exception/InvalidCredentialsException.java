package com.example.musicbooru.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends GenericException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
