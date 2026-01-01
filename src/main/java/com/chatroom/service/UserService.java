package com.chatroom.service;

import com.chatroom.model.User;
import com.chatroom.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

/**
 * UserService - Business logic for user operations
 *
 * Responsibilities:
 * - Validate user input
 * - Hash passwords securely using BCrypt
 * - Handle registration and login logic
 * - Communicate with repository for data access
 *
 * Security Note: Passwords are NEVER stored in plain text!
 * We use BCrypt which is a one-way hashing algorithm designed
 * specifically for password storage.
 */
public class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Register a new user
     *
     * Process:
     * 1. Validate username and password
     * 2. Check if username already exists
     * 3. Hash password using BCrypt
     * 4. Save user to database
     *
     * @param username - Unique username
     * @param password - Plain text password (will be hashed)
     * @return Registered User object
     * @throws IllegalArgumentException if validation fails
     */
    public User registerUser(String username, String password) {
        // Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }

        // Validate password
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Hash password using BCrypt
        // BCrypt automatically generates a salt and combines it with the hash
        // This makes each password unique even if two users have the same password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Create and save user
        User user = new User(username, hashedPassword);
        return userRepository.save(user);
    }

    /**
     * Login user - verify credentials
     *
     * Process:
     * 1. Find user by username
     * 2. Verify password using BCrypt
     * 3. Return user if valid
     *
     * @param username - Username
     * @param password - Plain text password
     * @return User object if credentials are valid
     * @throws IllegalArgumentException if credentials are invalid
     */
    public User loginUser(String username, String password) {
        // Find user by username
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        // Verify password using BCrypt
        // BCrypt.checkpw compares the plain text password with the hashed one
        // It extracts the salt from the hash and applies the same algorithm
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    /**
     * Find user by ID
     * Used by MessageService to verify user exists before sending message
     */
    public Optional<User> findUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Find user by username
     * Can be used for profile lookups
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}