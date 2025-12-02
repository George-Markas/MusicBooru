package com.example.musicbooru.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends GenericException{
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}