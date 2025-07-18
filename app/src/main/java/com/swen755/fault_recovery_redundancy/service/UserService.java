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
     */
    public User register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username);
        user.setPassword(passwordEncoder.encode(request.password)); // Securely encode password
        user.setEmail(request.email);
        user.setActive(true);
        user.generateStreamKey();
        return userRepository.save(user);
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
