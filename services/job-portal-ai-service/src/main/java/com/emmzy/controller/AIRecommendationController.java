package com.emmzy.controller;

import com.emmzy.dto.request.CandidateRankingRequest;
import com.emmzy.dto.request.JobMatchRequest;
import com.emmzy.dto.request.ResumeAnalysisRequest;
import com.emmzy.dto.response.CandidateRankingResponse;
import com.emmzy.dto.response.CandidateRecommendationResponse;
import com.emmzy.dto.response.JobMatchingResponse;
import com.emmzy.dto.response.JobRecommendationResponse;
import com.emmzy.dto.response.ResumeAnalysisResponse;
import com.emmzy.dto.response.SkillMatchResponse;
import com.emmzy.service.AIRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRecommendationController {

    private final AIRecommendationService aiRecommendationService;

    @GetMapping("/recommendations/jobs/{candidateId}")
    public ResponseEntity<List<JobRecommendationResponse>> getJobRecommendations(
            @PathVariable Long candidateId) {

        return ResponseEntity.ok(
                aiRecommendationService.getJobRecommendations(candidateId)
        );
    }

    @GetMapping("/recommendations/candidates/{jobId}")
    public ResponseEntity<List<CandidateRecommendationResponse>> getCandidateRecommendations(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                aiRecommendationService.getCandidateRecommendations(jobId)
        );
    }

    @PostMapping("/match/job")
    public ResponseEntity<JobMatchingResponse> matchJob(
            @RequestBody JobMatchRequest request) {

        return ResponseEntity.ok(
                aiRecommendationService.matchJob(request)
        );
    }

    @GetMapping("/skills/{candidateId}")
    public ResponseEntity<SkillMatchResponse> matchSkills(
            @PathVariable Long candidateId) {

        return ResponseEntity.ok(
                aiRecommendationService.matchSkills(candidateId)
        );
    }

    @PostMapping("/rank/candidates")
    public ResponseEntity<List<CandidateRankingResponse>> rankCandidates(
            @RequestBody CandidateRankingRequest request) {

        return ResponseEntity.ok(
                aiRecommendationService.rankCandidates(request)
        );
    }

    @PostMapping("/analyze/resume")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @RequestBody ResumeAnalysisRequest request) {

        return ResponseEntity.ok(
                aiRecommendationService.analyzeResume(request)
        );
    }
}

