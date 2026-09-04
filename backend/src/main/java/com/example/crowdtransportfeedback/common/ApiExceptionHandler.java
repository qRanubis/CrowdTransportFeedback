package com.example.crowdtransportfeedback.common;
import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){return ResponseEntity.status(e.status).body(Map.of("code",e.code,"message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(){return ResponseEntity.badRequest().body(Map.of("code","validation_error","message","Request validation failed"));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> fallback(){return ResponseEntity.status(500).body(Map.of("code","internal_error","message","An unexpected error occurred"));}
}
