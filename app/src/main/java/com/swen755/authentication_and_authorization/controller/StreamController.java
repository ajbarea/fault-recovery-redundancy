package com.swen755.authentication_and_authorization.controller;

import com.swen755.authentication_and_authorization.entity.User;
import com.swen755.authentication_and_authorization.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private final UserService userService;

    public StreamController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/start")
    //the streamkey is automatically passed by the nginx rtmp on_publish
    public ResponseEntity<?> start(@RequestParam("name") String streamKey) throws Exception {
        try{
            Optional<User> user = userService.findByStreamKey(streamKey);
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", "stream_"+user.get().getUsername()).build();
            //return ResponseEntity.ok().build();

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam("name") String streamKey) throws Exception {
        try {
            Optional<User> user = userService.findByStreamKey(streamKey);
            if (user.isPresent()) {
                // Stops the stream
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stream key not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/stream_{username}")
    public ResponseEntity<?> redirect()
    {
        return ResponseEntity.ok().build();
    }

}
