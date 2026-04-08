package com.thereplicant.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 * Returns RFC 7807 Problem Details format for all errors.
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (e.g., @Valid failed).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://thereplicant.blog/errors/validation"));
        problem.setProperty("errors", errors);

        return problem;
    }

    /**
     * Handle malformed JSON or type coercion errors.
     * Triggered when Jackson rejects invalid types (e.g., number for string field).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request body: check field types and JSON format");
        problem.setTitle("Malformed Request");
        problem.setType(URI.create("https://thereplicant.blog/errors/malformed-request"));

        return problem;
    }

    /**
     * Handle unsupported HTTP methods (e.g., PUT when only PATCH is mapped).
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint");
        problem.setTitle("Method Not Allowed");
        problem.setType(URI.create("https://thereplicant.blog/errors/method-not-allowed"));

        return problem;
    }

    /**
     * Handle authentication failures (invalid credentials).
     * OWASP: Always return the same generic message to prevent user enumeration.
     * The message is hardcoded here regardless of the actual exception detail
     * to guarantee anti-enumeration even if someone modifies AuthService.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://thereplicant.blog/errors/authentication"));

        return problem;
    }

    /**
     * Handle setup already completed (conflict).
     * Uses custom exception instead of generic IllegalStateException
     * to avoid catching unrelated exceptions as 409.
     */
    @ExceptionHandler(SetupAlreadyCompletedException.class)
    public ProblemDetail handleSetupAlreadyCompleted(SetupAlreadyCompletedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());
        problem.setTitle("Setup Already Completed");
        problem.setType(URI.create("https://thereplicant.blog/errors/setup-conflict"));

        return problem;
    }

    /**
     * Handle resource not found (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://thereplicant.blog/errors/not-found"));

        return problem;
    }

    /**
     * Handle duplicate resource conflict (409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://thereplicant.blog/errors/conflict"));

        return problem;
    }

    /**
     * Handle illegal state (e.g., deleting category with posts).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());
        problem.setTitle("Operation Not Allowed");
        problem.setType(URI.create("https://thereplicant.blog/errors/illegal-state"));

        return problem;
    }

    /**
     * Catch-all for unexpected exceptions.
     * Logs the full error but returns generic message to user.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://thereplicant.blog/errors/internal"));

        return problem;
    }
}
