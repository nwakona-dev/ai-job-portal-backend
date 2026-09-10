package com.emmzy.controller;

import com.emmzy.domain.UserRole;
import com.emmzy.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping
    public ApiResponse HomeController() {
        return new ApiResponse(
                "Welcome to the Job Portal API Service! This service is responsible for managing job postings and applications. " +
                         UserRole.ROLE_EMPLOYER,
                true);
    }
}
