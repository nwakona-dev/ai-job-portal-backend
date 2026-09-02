package com.emmzy.service;

import com.emmzy.payload.AuthResponse;
import com.emmzy.payload.LoginRequest;
import com.emmzy.payload.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest req) throws Exception;
    AuthResponse login(LoginRequest req) throws Exception;
}
