package com.thereplicant.api.exception;

/**
 * Thrown when first-time admin setup is attempted but setup has already been
 * completed.
 * 
 * Replaces generic {@link IllegalStateException} to avoid catching unrelated
 * exceptions in the GlobalExceptionHandler (OWASP: fail securely).
 */
public class SetupAlreadyCompletedException extends RuntimeException {

    public SetupAlreadyCompletedException() {
        super("Setup already completed. Admin user exists.");
    }

    public SetupAlreadyCompletedException(String message) {
        super(message);
    }
}
