package com.emmzy.controller;

import com.emmzy.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping
    public ResponseEntity<ApiResponse> HomeController() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Service for managing candidate resume");
        apiResponse.setStatus(true);

        return ResponseEntity.ok(apiResponse);
    }
}
