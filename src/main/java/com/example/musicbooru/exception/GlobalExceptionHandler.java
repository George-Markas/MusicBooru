package com.example.musicbooru.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            GenericException e,
            HttpServletRequest request
    ) {

        ErrorResponse error = new ErrorResponse(
                e.getStatus().value(),
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, e.getStatus());
    }
}
