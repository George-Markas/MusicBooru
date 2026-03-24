package org.example.musicbooru.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class GenericException extends RuntimeException {
    @Getter
    private final HttpStatusCode status;

    public GenericException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public GenericException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
    }
}
