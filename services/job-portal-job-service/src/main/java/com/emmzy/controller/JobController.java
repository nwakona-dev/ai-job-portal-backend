package com.emmzy.controller;

import com.emmzy.dto.ApiResponse;
import com.emmzy.dto.JobRequest;
import com.emmzy.dto.JobResponse;
import com.emmzy.payload.JobSearchRequest;
import com.emmzy.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:3000",
        allowedHeaders = "*",
        methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.PATCH,
                RequestMethod.DELETE,
                RequestMethod.OPTIONS
        }
)
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody @Valid JobRequest jobRequest
    ) throws Exception {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobService.createJob(
                        employerId,
                        jobRequest
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable Long id
    ) throws Exception {

        return ResponseEntity.ok(
                jobService.getJobById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
            @ModelAttribute JobSearchRequest req,
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long userId
    ) {

        return ResponseEntity.ok(
                jobService.getJobs(req, userId)
        );
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<JobResponse>> getJobsByCompany(
            @PathVariable Long companyId
    ) {

        return ResponseEntity.ok(
                jobService.getJobsByCompany(companyId)
        );
    }

    @GetMapping("/employer/{employerId}")
    public ResponseEntity<List<JobResponse>> getJobsByEmployer(
            @PathVariable Long employerId
    ) {

        return ResponseEntity.ok(
                jobService.getJobsByEmployer(employerId)
        );
    }

    @GetMapping("/admin")
    public ResponseEntity<List<JobResponse>> getAllJobsAdmin() {

        return ResponseEntity.ok(
                jobService.getAllJobsAdmin()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody @Valid JobRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                jobService.updateJob(
                        id,
                        employerId,
                        req
                )
        );
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<JobResponse> publishJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {

        return ResponseEntity.ok(
                jobService.publishJob(
                        id,
                        employerId
                )
        );
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<JobResponse> closeJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {

        return ResponseEntity.ok(
                jobService.closeJob(
                        id,
                        employerId
                )
        );
    }

    /*
     * SAVE / UNSAVE JOB
     *
     * PATCH /api/jobs/{id}/save
     *
     * Header:
     * X-User-Id: <current user id>
     *
     * Body:
     * {
     *     "saved": true
     * }
     */
    @PatchMapping("/{id}/save")
    public ResponseEntity<ApiResponse> toggleSaveJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SaveJobRequest request
    ) throws Exception {

        boolean saved = jobService.toggleSaveJob(
                id,
                userId,
                request.saved()
        );

        return ResponseEntity.ok(
                new ApiResponse(
                        saved
                                ? "Job saved successfully"
                                : "Job removed from saved jobs",
                        true
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {

        jobService.deleteJob(
                id,
                employerId
        );

        return ResponseEntity.ok(
                new ApiResponse(
                        "Job deleted successfully",
                        true
                )
        );
    }

    /*
     * Small request DTO kept inside the controller because it is
     * only needed by this endpoint.
     */
    public record SaveJobRequest(
            boolean saved
    ) {
    }
}