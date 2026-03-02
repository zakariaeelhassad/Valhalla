package com.example.backend.exception;

import com.example.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildError(ex.getMessage(), "Resource Not Found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientBudgetException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBudgetException(InsufficientBudgetException ex) {
        return buildError(ex.getMessage(), "Insufficient Budget", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TeamFullException.class)
    public ResponseEntity<ErrorResponse> handleTeamFullException(TeamFullException ex) {
        return buildError(ex.getMessage(), "Team Full", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransferException(InvalidTransferException ex) {
        return buildError(ex.getMessage(), "Invalid Transfer", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildError(ex.getMessage(), "Bad Request", HttpStatus.BAD_REQUEST);
    }

    // Handles RuntimeException (e.g. "Email already in use") as 400 Bad Request
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return buildError(ex.getMessage(), "Bad Request", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        return buildError(ex.getMessage(), "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildError(String message, String error, HttpStatus status) {
        return new ResponseEntity<>(new ErrorResponse(message, error, status.value()), status);
    }
}
