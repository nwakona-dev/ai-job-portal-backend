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

    /**
     * Get job recommendations for a candidate.
     *
     * GET /api/ai/recommendations/jobs/{candidateId}
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
     * Get candidate recommendations for a job.
     *
     * GET /api/ai/recommendations/candidates/{jobId}
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
     * Match a candidate against a specific job.
     *
     * POST /api/ai/match/job
     */
    @PostMapping("/match/job")
    public ResponseEntity<JobMatchingResponse> matchJob(
            @RequestBody JobMatchRequest request
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.matchJob(request)
        );
    }

    /**
     * Match a candidate's skills against a job.
     *
     * GET /api/ai/skills/{candidateId}
     */
    @GetMapping("/skills/{candidateId}")
    public ResponseEntity<SkillMatchResponse> matchSkills(
            @PathVariable Long candidateId
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.matchSkills(candidateId)
        );
    }

    /**
     * Rank candidates for a specific job.
     *
     * POST /api/ai/rank/candidates
     */
    @PostMapping("/rank/candidates")
    public ResponseEntity<CandidateRankingResponse> rankCandidates(
            @RequestBody CandidateRankingRequest request
    ) {
        return ResponseEntity.ok(
                (CandidateRankingResponse) aiRecommendationService.rankCandidates(request)
        );
    }

    /**
     * Analyze a candidate resume.
     *
     * POST /api/ai/analyze/resume
     */
    @PostMapping("/analyze/resume")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @RequestBody ResumeAnalysisRequest request
    ) {
        return ResponseEntity.ok(
                aiRecommendationService.analyzeResume(request)
        );
    }
}
