package com.emmzy.controller;

import com.emmzy.dto.ApiResponse;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.ProjectResponse;
import com.emmzy.model.Application;
import com.emmzy.payload.CompanyApplicationFilterRequest;
import com.emmzy.payload.CreateApplicationRequest;
import com.emmzy.payload.UpdateApplicationStatusRequest;
import com.emmzy.payload.WithdrawApplicationRequest;
import com.emmzy.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication (
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid CreateApplicationRequest createApplicationRequest
    ) throws Exception {
        return ResponseEntity.ok(
                applicationService.createApplication(candidateId, createApplicationRequest)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id) throws Exception {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @RequestHeader("X-User-Id") Long candidateId) {
        return ResponseEntity.ok(applicationService.getMyApplication(candidateId));
    }

    //can skip
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
            @PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationForJob(jobId));
    }

    @GetMapping("/company")
    public ResponseEntity<List<ApplicationResponse>> getMyApplicationsForCompany(
            @RequestHeader("X-User-Id") Long userId,
            @ModelAttribute CompanyApplicationFilterRequest filter) throws Exception {
        return ResponseEntity.ok(applicationService.getApplicationForCompany(
                userId, filter));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody @Valid UpdateApplicationStatusRequest req)
        throws Exception {
        return ResponseEntity.ok(applicationService.updateStatus(
                id,
                employerId,
                req.getStatus()));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid WithdrawApplicationRequest req)
            throws Exception {
        return ResponseEntity.ok(applicationService.withdraw(
                id,
                candidateId,
                req));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ApplicationResponse> toggleStar(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId)
            throws Exception {
        return ResponseEntity.ok(applicationService.toggleStar(id, employerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteApplication (
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long candidateId)
        throws Exception {
        applicationService.deleteApplication(id, candidateId);
        return ResponseEntity.ok(new ApiResponse("Application deleted successfully", true));
    }
}
