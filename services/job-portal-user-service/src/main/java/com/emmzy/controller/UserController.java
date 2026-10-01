package com.emmzy.controller;

import com.emmzy.dto.response.UserResponse;
import com.emmzy.mapper.UserMapper;
import com.emmzy.model.User;
import com.emmzy.payload.UpdateUserRequest;
import com.emmzy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(
            Authentication authentication
    ) throws Exception {

        String email = authentication.getName();

        User user = userService.getUserByEmail(email);

        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateUserRequest req
    ) throws Exception {

        String email = authentication.getName();

        return ResponseEntity.ok(
                userService.updateProfile(email, req)
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId
    ) throws Exception {

        User user = userService.getUserById(userId);

        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                UserMapper.toDTOList(userService.getAllUsers())
        );
    }

    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable Long userId
    ) throws Exception {

        return ResponseEntity.ok(
                userService.suspendUser(userId)
        );
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Long userId
    ) throws Exception {

        return ResponseEntity.ok(
                userService.activateUser(userId)
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponse> deleteUser(
            @PathVariable Long userId
    ) throws Exception {

        return ResponseEntity.ok(
                userService.deleteUser(userId)
        );
    }
}