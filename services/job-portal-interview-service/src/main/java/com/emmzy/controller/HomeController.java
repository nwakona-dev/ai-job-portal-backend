package com.emmzy.controller;

import com.emmzy.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

        @GetMapping
        public ResponseEntity<?> HomeController() {
            String message = "Service for scheduling job interview," +
                    "tracking application status," +
                    " and maintaining application history";
            ApiResponse res = new ApiResponse(message, true);
            return ResponseEntity.ok(res);
        }
}
