package com.example.musicbooru.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(GenericException e, HttpServletRequest request) {
        log.warn("Generic exception: {}", e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(
                e.getStatus(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e,
                                                                         HttpServletRequest request) {

        log.warn("Resource not found: {} with ID '{}'", e.getResourceType(), e.getResourceId(), e);

        ErrorResponse response = new ErrorResponse(
                e.getStatus(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException e,
                                                                           HttpServletRequest request) {

        log.info("Invalid credentials: {}", e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(
                e.getStatus(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
                                                                              HttpServletRequest request) {

        log.warn("Max upload size exceeded: {}", e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONTENT_TOO_LARGE,
                "Max upload size exceeded",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                               HttpServletRequest request) {

        log.warn("Argument validation failed: {}", e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: {}", e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
