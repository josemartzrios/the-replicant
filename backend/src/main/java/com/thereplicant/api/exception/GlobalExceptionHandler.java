package com.thereplicant.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
