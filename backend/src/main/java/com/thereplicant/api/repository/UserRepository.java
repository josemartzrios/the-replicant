package com.thereplicant.api.repository;

import com.thereplicant.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 * 
 * Uses Spring Data JPA for automatic query generation.
 * Custom methods follow naming conventions for auto-implementation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (used for login).
     * Email is the unique identifier for authentication.
     * 
     * @param email user's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists (for registration validation).
     * Prevents duplicate accounts.
     * 
     * @param email email to check
     * @return true if email is already registered
     */
    boolean existsByEmail(String email);
}
