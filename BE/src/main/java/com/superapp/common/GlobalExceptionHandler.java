package com.superapp.common;

import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiErrorResponse> handleDateParse(DateTimeParseException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
            "invalid_datetime",
            "Invalid datetime format. Use ISO-8601, e.g. 2026-05-23T00:00:00Z"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatusException(ResponseStatusException ex) {
        String reason = ex.getReason() == null ? ex.getStatus().getReasonPhrase() : ex.getReason();
        ApiErrorResponse body = new ApiErrorResponse(ex.getStatus().name().toLowerCase(), reason);
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}

