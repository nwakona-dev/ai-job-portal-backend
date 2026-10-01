package com.emmzy.controller;

import com.emmzy.payload.AuthResponse;
import com.emmzy.payload.LoginRequest;
import com.emmzy.payload.SignupRequest;
import com.emmzy.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/user")
    public ResponseEntity<AuthResponse> signup(
            @RequestBody @Valid SignupRequest req
    ) throws Exception{
        return ResponseEntity.ok(authService.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest req
    ) throws Exception{
        return ResponseEntity.ok(authService.login(req));
    }
}
