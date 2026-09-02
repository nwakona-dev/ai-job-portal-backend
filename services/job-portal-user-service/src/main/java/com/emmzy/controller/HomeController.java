package com.emmzy.controller;

import com.emmzy.domain.UserRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping
    public String HomeController() {
        return "Job Portal User Service is running!......."+ UserRole.ROLE_JOB_SEEKER;
    }
}
