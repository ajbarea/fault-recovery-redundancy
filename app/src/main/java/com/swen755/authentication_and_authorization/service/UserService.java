package com.swen755.authentication_and_authorization.service;

import com.swen755.authentication_and_authorization.dto.RegisterRequest;
import com.swen755.authentication_and_authorization.entity.User;
import com.swen755.authentication_and_authorization.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request)
    {
        User user = new User();
        user.setUsername(request.username);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setEmail(request.email);
        user.setActive(true);
        user.generateStreamKey();
        return userRepository.save(user);

    }

    public Optional<User> findByStreamKey(String streamKey) throws Exception {
        Optional<User> user = userRepository.findByStreamKey(streamKey);
        if(user.isEmpty())
            throw new Exception("User not found with the provided stream key");

        return user;
    }
}
