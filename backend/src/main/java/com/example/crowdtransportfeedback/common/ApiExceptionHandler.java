package com.example.crowdtransportfeedback.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException error) {
        return ResponseEntity.status(error.status)
            .body(Map.of("code", error.code, "message", error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation() {
        return ResponseEntity.badRequest()
            .body(Map.of("code", "validation_error", "message", "Request validation failed"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> malformedJson() {
        return ResponseEntity.badRequest()
            .body(Map.of("code", "validation_error", "message", "Request validation failed"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integrityViolation() {
        return ResponseEntity.badRequest()
            .body(Map.of("code", "validation_error", "message", "Request validation failed"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> fallback() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", "internal_error", "message", "An unexpected error occurred"));
    }
}
