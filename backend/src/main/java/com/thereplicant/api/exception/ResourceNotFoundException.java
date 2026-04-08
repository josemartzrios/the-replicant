package com.thereplicant.api.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 Not Found.
 *
 * OWASP: Returns a generic message to avoid leaking internal details.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " with ID '" + id + "' not found");
    }
}
