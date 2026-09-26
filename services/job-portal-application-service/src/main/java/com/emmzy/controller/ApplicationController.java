package com.emmzy.controller;

import com.emmzy.model.Application;
import com.emmzy.model.ApplicationStatus;
import com.emmzy.service.ApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<Application> createApplication(
            @RequestHeader("X-User-Id") Long jobSeekerId,
            @RequestParam Long jobId,
            @RequestParam Long resumeId
    ) throws Exception {

        Application application =
                applicationService.createApplication(
                        jobId,
                        jobSeekerId,
                        resumeId
                );

        return ResponseEntity.ok(application);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Application>> getMyApplications(
            @RequestHeader("X-User-Id") Long jobSeekerId
    ) {

        return ResponseEntity.ok(
                applicationService.getApplicationsByJobSeeker(
                        jobSeekerId
                )
        );
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> getApplicationsByJob(
            @PathVariable Long jobId
    ) {

        return ResponseEntity.ok(
                applicationService.getApplicationsByJob(jobId)
        );
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<Application> getApplication(
            @PathVariable Long applicationId,
            @RequestHeader("X-User-Id") Long jobSeekerId
    ) throws Exception {

        return ResponseEntity.ok(
                applicationService.getApplication(
                        applicationId,
                        jobSeekerId
                )
        );
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Application> updateStatus(
            @PathVariable Long applicationId,
            @RequestParam ApplicationStatus status
    ) throws Exception {

        return ResponseEntity.ok(
                applicationService.updateStatus(
                        applicationId,
                        status
                )
        );
    }

    @PatchMapping("/{applicationId}/withdraw")
    public ResponseEntity<Void> withdrawApplication(
            @PathVariable Long applicationId,
            @RequestHeader("X-User-Id") Long jobSeekerId
    ) throws Exception {

        applicationService.withdrawApplication(
                applicationId,
                jobSeekerId
        );

        return ResponseEntity.noContent().build();
    }
}