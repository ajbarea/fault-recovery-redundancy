package com.swen755.fault_recovery_redundancy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.swen755.fault_recovery_redundancy.entity.User;
import com.swen755.fault_recovery_redundancy.service.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private final UserService userService;

    public StreamController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Starts a stream for a user by stream key. Redirects to user stream if found.
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam("name") String streamKey) throws Exception {
        try {
            Optional<User> user = userService.findByStreamKey(streamKey);
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "stream_" + user.get().getUsername())
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Stops a stream for a user by stream key.
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam("name") String streamKey) throws Exception {
        try {
            Optional<User> user = userService.findByStreamKey(streamKey);
            if (user.isPresent()) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stream key not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Endpoint for stream redirection (placeholder).
     */
    @GetMapping("/stream_{username}")
    public ResponseEntity<?> redirect() {
        return ResponseEntity.ok().build();
    }

}
