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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRecommendationController {

    private final AIRecommendationService aiRecommendationService;

    /**
     * Get recommended jobs for a candidate.
     */
    @GetMapping("/recommendations/jobs/{candidateId}")
    public ResponseEntity<List<JobRecommendationResponse>> getJobRecommendations(
            @PathVariable Long candidateId
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.getJobRecommendations(candidateId)
        );
    }

    /**
     * Match a candidate against a specific job.
     */
    @PostMapping("/matching/job")
    public ResponseEntity<JobMatchingResponse> matchJob(
            @RequestBody JobMatchRequest request
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.matchJob(request)
        );
    }

    /**
     * Get skill matching information for a candidate.
     */
    @GetMapping("/skills/{candidateId}")
    public ResponseEntity<SkillMatchResponse> getSkillMatch(
            @PathVariable Long candidateId
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.getSkillMatch(candidateId)
        );
    }

    /**
     * Rank candidates for a job.
     */
    @PostMapping("/candidates/rank")
    public ResponseEntity<List<CandidateRankingResponse>> rankCandidates(
            @RequestBody CandidateRankingRequest request
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.rankCandidates(request)
        );
    }

    /**
     * Get recommended candidates for a job.
     */
    @GetMapping("/recommendations/candidates/{jobId}")
    public ResponseEntity<List<CandidateRecommendationResponse>> getCandidateRecommendations(
            @PathVariable Long jobId
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.getCandidateRecommendations(jobId)
        );
    }

    /**
     * Analyze a resume using the existing AI recommendation engine.
     */
    @PostMapping("/resume/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @RequestBody ResumeAnalysisRequest request
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.analyzeResume(request)
        );
    }
}