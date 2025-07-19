package com.swen755.fault_recovery_redundancy.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.swen755.fault_recovery_redundancy.dto.RegisterRequest;
import com.swen755.fault_recovery_redundancy.entity.User;
import com.swen755.fault_recovery_redundancy.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with encoded password and generated stream key.
     * Throws exception if user already exists.
     */
    public User register(RegisterRequest request) {
        try {
            // Check if user already exists by username or email
            Optional<User> existingUser = userRepository.findByUsername(request.username);
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("Username already exists");
            }

            // Check if email already exists
            if (userRepository.findByEmail(request.email).isPresent()) {
                throw new IllegalArgumentException("Email already registered");
            }

            User user = new User();
            user.setUsername(request.username);
            user.setPassword(passwordEncoder.encode(request.password)); // Securely encode password
            user.setEmail(request.email);
            user.setActive(true);
            user.generateStreamKey();
            return userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle database constraint violations
            if (e.getMessage().contains("UK6dotkott2kjsp8vw4d0m25fb7") || e.getMessage().contains("email")) {
                throw new IllegalArgumentException("Email already registered");
            } else if (e.getMessage().contains("username")) {
                throw new IllegalArgumentException("Username already exists");
            } else {
                throw new IllegalArgumentException("User already exists");
            }
        } catch (IllegalArgumentException e) {
            // Re-throw our custom validation errors
            throw e;
        } catch (Exception e) {
            // Handle any other unexpected errors
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new user or logs in an existing user if credentials match.
     * Returns a result object indicating whether the user was registered or logged
     * in.
     */
    public RegisterOrLoginResult registerOrLogin(RegisterRequest request) {
        try {
            // Check if user already exists by username
            Optional<User> existingUserByUsername = userRepository.findByUsername(request.username);
            if (existingUserByUsername.isPresent()) {
                User existingUser = existingUserByUsername.get();
                // Check if password matches
                if (passwordEncoder.matches(request.password, existingUser.getPassword())) {
                    // Password matches, log them in
                    return new RegisterOrLoginResult(existingUser, false,
                            "Login successful - user already existed with matching credentials");
                } else {
                    // Password doesn't match
                    throw new IllegalArgumentException("Username already exists with different password");
                }
            }

            // Check if email already exists with different username
            Optional<User> existingUserByEmail = userRepository.findByEmail(request.email);
            if (existingUserByEmail.isPresent()) {
                throw new IllegalArgumentException("Email already registered with different username");
            }

            // No existing user, proceed with registration
            User user = new User();
            user.setUsername(request.username);
            user.setPassword(passwordEncoder.encode(request.password));
            user.setEmail(request.email);
            user.setActive(true);
            user.generateStreamKey();
            User savedUser = userRepository.save(user);
            return new RegisterOrLoginResult(savedUser, true, "Registration successful");

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle database constraint violations
            if (e.getMessage().contains("UK6dotkott2kjsp8vw4d0m25fb7") || e.getMessage().contains("email")) {
                throw new IllegalArgumentException("Email already registered");
            } else if (e.getMessage().contains("username")) {
                throw new IllegalArgumentException("Username already exists");
            } else {
                throw new IllegalArgumentException("User already exists");
            }
        } catch (IllegalArgumentException e) {
            // Re-throw our custom validation errors
            throw e;
        } catch (Exception e) {
            // Handle any other unexpected errors
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Result object for registerOrLogin operation
     */
    public static class RegisterOrLoginResult {
        private final User user;
        private final boolean wasRegistered;
        private final String message;

        public RegisterOrLoginResult(User user, boolean wasRegistered, String message) {
            this.user = user;
            this.wasRegistered = wasRegistered;
            this.message = message;
        }

        public User getUser() {
            return user;
        }

        public boolean wasRegistered() {
            return wasRegistered;
        }

        public boolean wasLoggedIn() {
            return !wasRegistered;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Authenticates a user and returns their information if credentials are valid.
     */
    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    /**
     * Finds a user by username.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    /**
     * Finds a user by their stream key, or throws if not found.
     */
    public Optional<User> findByStreamKey(String streamKey) throws Exception {
        Optional<User> user = userRepository.findByStreamKey(streamKey);
        if (user.isEmpty())
            throw new Exception("User not found with the provided stream key");

        return user;
    }
}
