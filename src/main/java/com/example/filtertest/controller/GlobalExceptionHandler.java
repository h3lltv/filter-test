package com.example.filtertest.controller;

import com.example.filtertest.dto.ErrorResponse;
import java.time.Instant;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ErrorResponse> handleBadInput(ServerWebInputException ex, ServerHttpRequest request) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return errorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(PatternSyntaxException.class)
    ResponseEntity<ErrorResponse> handleRegexSyntax(PatternSyntaxException ex, ServerHttpRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Invalid regular expression: " + ex.getMessage(), request);
    }

    @ExceptionHandler(com.google.re2j.PatternSyntaxException.class)
    ResponseEntity<ErrorResponse> handleRe2jSyntax(com.google.re2j.PatternSyntaxException ex, ServerHttpRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Invalid regular expression: " + ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerHttpRequest request) {
        log.error("Unexpected error handling request {}", request.getPath(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message, ServerHttpRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getPath().value());
        return ResponseEntity.status(status).body(body);
    }
}
