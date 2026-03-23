package org.example.musicbooru.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            GenericException e,
            HttpServletRequest request) {

        logger.error("Exception occurred: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                e.getStatus(),
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, e.getStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e,
            HttpServletRequest request) {

        logger.error("Resource not found: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request) {

        logger.error("Illegal argument: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
