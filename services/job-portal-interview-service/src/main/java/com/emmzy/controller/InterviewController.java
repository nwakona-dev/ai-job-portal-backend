package com.emmzy.controller;

import com.emmzy.dto.CreateInterviewRequest;
import com.emmzy.dto.HiringDecisionRequest;
import com.emmzy.dto.InterviewFeedbackRequest;
import com.emmzy.dto.InterviewResponse;
import com.emmzy.dto.UpdateInterviewRequest;
import com.emmzy.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewResponse> createInterview(
            @RequestHeader("X-User-Id") Long employerId,
            @Valid @RequestBody CreateInterviewRequest request
    ) throws Exception {

        InterviewResponse response =
                interviewService.createInterview(
                        employerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterviewById(
            @PathVariable Long id
    ) throws Exception {

        return ResponseEntity.ok(
                interviewService.getInterviewById(id)
        );
    }

    @GetMapping("/candidate")
    public ResponseEntity<List<InterviewResponse>> getCandidateInterviews(
            @RequestHeader("X-User-Id") Long candidateId
    ) {

        return ResponseEntity.ok(
                interviewService.getCandidateInterviews(
                        candidateId
                )
        );
    }

    @GetMapping("/employer")
    public ResponseEntity<List<InterviewResponse>> getEmployerInterviews(
            @RequestHeader("X-User-Id") Long employerId
    ) {

        return ResponseEntity.ok(
                interviewService.getEmployerInterviews(
                        employerId
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> updateInterview(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @Valid @RequestBody UpdateInterviewRequest request
    ) throws Exception {

        return ResponseEntity.ok(
                interviewService.updateInterview(
                        id,
                        employerId,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {

        return ResponseEntity.ok(
                interviewService.cancelInterview(
                        id,
                        employerId
                )
        );
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<InterviewResponse> addFeedback(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @Valid @RequestBody InterviewFeedbackRequest request
    ) throws Exception {

        return ResponseEntity.ok(
                interviewService.addFeedback(
                        id,
                        employerId,
                        request
                )
        );
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<InterviewResponse> makeHiringDecision(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @Valid @RequestBody HiringDecisionRequest request
    ) throws Exception {

        return ResponseEntity.ok(
                interviewService.makeHiringDecision(
                        id,
                        employerId,
                        request
                )
        );
    }
}